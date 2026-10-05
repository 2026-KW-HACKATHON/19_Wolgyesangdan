import { useCallback, useEffect, useRef, useState } from 'react'
import { checkLocation } from '../api/verification'
import type { Coords, LocationStatus } from '../types/verification'

/** 이보다 오차가 크면 "위치가 정확하지 않아요" */
const MAX_ACCURACY_M = 100

// 개발 모드에서 ?mock=inside|outside|denied|inaccurate|unavailable 로 화면 상태를 바로 확인할 수 있다.
type MockStatus = Exclude<LocationStatus, 'locating'>
const MOCK_STATUSES: MockStatus[] = ['inside', 'outside', 'denied', 'inaccurate', 'unavailable']
const MOCK_COORDS: Record<'inside' | 'outside' | 'inaccurate', Coords> = {
  inside: { lat: 37.6197, lng: 127.059, accuracy: 20 },
  outside: { lat: 37.6542, lng: 127.0568, accuracy: 25 },
  inaccurate: { lat: 37.6197, lng: 127.059, accuracy: 340 },
}

function readMockStatus(): MockStatus | null {
  if (!import.meta.env.DEV) return null
  const mock = new URLSearchParams(window.location.search).get('mock') as MockStatus | null
  return mock && MOCK_STATUSES.includes(mock) ? mock : null
}

function getPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject({ code: 2 })
      return
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 10000 })
  })
}

export interface NeighborhoodLocation {
  status: LocationStatus
  coords: Coords | null
  dongName: string | null
  /** 위치를 다시 찾는다 (재측위) */
  locate: () => void
}

/** GPS로 현재 위치를 찾고, 월계1동 안인지 판정한다. 화면에 들어오자마자 한 번 측위한다. */
export function useNeighborhoodLocation(): NeighborhoodLocation {
  const [status, setStatus] = useState<LocationStatus>('locating')
  const [coords, setCoords] = useState<Coords | null>(null)
  const [dongName, setDongName] = useState<string | null>(null)
  // 재측위 중 이전 요청 결과가 늦게 도착해도 덮어쓰지 않도록 요청 번호로 구분한다
  const requestId = useRef(0)

  // 측위 → 판정. state는 await 이후에만 바꾼다 (초기 측위를 effect에서 바로 부르기 때문).
  const measure = useCallback(async () => {
    const id = ++requestId.current
    const isLatest = () => id === requestId.current

    const mock = readMockStatus()
    try {
      let next: Coords
      if (mock) {
        await new Promise((resolve) => setTimeout(resolve, 400))
        if (mock === 'denied' || mock === 'unavailable') throw { code: mock === 'denied' ? 1 : 3 }
        next = MOCK_COORDS[mock]
      } else {
        const { coords: c } = await getPosition()
        next = { lat: c.latitude, lng: c.longitude, accuracy: c.accuracy }
      }
      if (!isLatest()) return
      setCoords(next)

      if (next.accuracy > MAX_ACCURACY_M) {
        setStatus('inaccurate')
        return
      }
      const result = await checkLocation(next)
      if (!isLatest()) return
      setDongName(result.dongName)
      setStatus(result.inside ? 'inside' : 'outside')
    } catch (error) {
      if (!isLatest()) return
      const code = (error as { code?: number }).code
      setStatus(code === 1 ? 'denied' : 'unavailable')
    }
  }, [])

  const locate = useCallback(() => {
    setStatus('locating')
    setDongName(null)
    void measure()
  }, [measure])

  useEffect(() => {
    // 진입 즉시 측위 (초기 state가 이미 locating). 결과는 콜백에서 비동기로 반영된다.
    const ticket = requestId
    void Promise.resolve().then(measure)
    // 화면을 떠난 뒤 도착한 결과는 버린다
    return () => {
      ticket.current++
    }
  }, [measure])

  return { status, coords, dongName, locate }
}
