import type { Coords, LocationCheckResult, PriorityType } from '../types/verification'

// 로그인·인증 플로우 v2(GPS 동네 인증 + 우선배정 서류) API.
// 백엔드 미구현이라 전부 임시 구현이다. 실제 API가 생기면 apiFetch로 교체한다.
//   POST /verification/location          { lat, lng, accuracy } → { inside, dongName, verifiedAt }
//   POST /verification/priority/files    (multipart)            → { fileId }
//   POST /verification/priority          { type, docType, fileIds[] } → { status: 'submitted' }

const fakeLatency = () => new Promise((resolve) => setTimeout(resolve, 500))

// 임시 판정용 — 월계1동 중심(광운대 인근)에서 1km 안이면 월계1동으로 본다.
// 실제 판정은 서버가 행정동 경계 폴리곤으로 한다 (클라이언트 판정은 신뢰하지 않음).
const WOLGYE1_CENTER = { lat: 37.6235, lng: 127.0605 }
const MOCK_RADIUS_M = 1000

function distanceMeters(a: { lat: number; lng: number }, b: { lat: number; lng: number }) {
  const R = 6371000
  const toRad = (deg: number) => (deg * Math.PI) / 180
  const dLat = toRad(b.lat - a.lat)
  const dLng = toRad(b.lng - a.lng)
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(h))
}

/** 현재 좌표가 월계1동 안인지 미리 확인 (결과 행 표시용). TODO: 서버 판정 API로 교체 */
export async function checkLocation(coords: Coords): Promise<LocationCheckResult> {
  await fakeLatency()
  const inside = distanceMeters(coords, WOLGYE1_CENTER) <= MOCK_RADIUS_M
  return { inside, dongName: inside ? '서울 노원구 월계1동' : '다른 동네' }
}

/** "이 위치로 인증하기" — 동네 인증 확정. TODO: POST /verification/location */
export async function verifyLocation(coords: Coords): Promise<LocationCheckResult & { verifiedAt: string }> {
  const result = await checkLocation(coords)
  if (!result.inside) throw new Error('월계1동 안에서 다시 시도해 주세요')
  return { ...result, verifiedAt: new Date().toISOString() }
}

/**
 * 우선배정 인증 신청 (서류 업로드 포함). TODO: files 업로드 → POST /verification/priority
 * 기초수급자 여부는 어떤 응답에서도 다른 사용자에게 노출하면 안 된다 — 대기열 정렬은 서버에서만 한다.
 */
export async function submitPriorityVerification(input: { type: PriorityType; docType: string; files: File[] }) {
  await fakeLatency()
  return { status: 'submitted' as const, docCount: input.files.length }
}
