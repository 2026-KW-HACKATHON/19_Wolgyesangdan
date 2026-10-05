import type {
  Coords,
  LocationCheckResult,
  MyVerification,
  VerificationCreateRequest,
  VerificationCreateResponse,
} from '../types/verification'
import { ApiError, apiFetch } from './client'

// 로그인·인증 플로우 v2(GPS 동네 인증 + 우선배정 서류) API.
//   POST /verifications/neighborhood   (본문 없음)                      → 동네 인증, 바로 APPROVED
//   POST /verifications                { verificationType, documentType } → 우선배정 인증 신청, PENDING
//   GET  /verifications/me                                               → 유형별 내 인증 상태

const fakeLatency = () => new Promise((resolve) => setTimeout(resolve, 500))

// 월계1동 안인지는 프론트가 판정한다 (서버는 좌표를 받지 않는다).
// 월계1동 중심(광운대 인근)에서 1km 안이면 월계1동으로 본다.
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

/** 현재 좌표가 월계1동 안인지 확인 (결과 행 표시용) */
export async function checkLocation(coords: Coords): Promise<LocationCheckResult> {
  await fakeLatency()
  const inside = distanceMeters(coords, WOLGYE1_CENTER) <= MOCK_RADIUS_M
  return { inside, dongName: inside ? '서울 노원구 월계1동' : '다른 동네' }
}

/**
 * "이 위치로 인증하기" — 월계1동 안이면 동네 인증을 기록한다 (POST /verifications/neighborhood).
 * 이미 동네 인증이 돼 있으면 실패가 아니라 인증된 것으로 본다.
 */
export async function verifyLocation(coords: Coords): Promise<LocationCheckResult> {
  const result = await checkLocation(coords)
  if (!result.inside) throw new Error('월계1동 안에서 다시 시도해 주세요')
  try {
    await apiFetch<VerificationCreateResponse>('/verifications/neighborhood', { method: 'POST' })
  } catch (e) {
    if (!(e instanceof ApiError && e.code === 'VERIFICATION_ALREADY_APPROVED')) throw e
  }
  return result
}

/**
 * 우선배정 인증 신청 (POST /verifications). 항상 PENDING으로 접수되고 운영진이 확인 후 승인한다.
 * 기초수급자 여부는 어떤 응답에서도 다른 사용자에게 노출하면 안 된다 — 대기열 정렬은 서버에서만 한다.
 */
export function createVerification(request: VerificationCreateRequest) {
  return apiFetch<VerificationCreateResponse>('/verifications', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

/** 내 인증 현황 (GET /verifications/me). 신청한 유형만, 유형마다 가장 최근 건 기준으로 내려온다. */
export function getMyVerifications() {
  return apiFetch<MyVerification[]>('/verifications/me')
}
