import { putToS3 } from '../lib/s3Upload'
import type {
  Coords,
  DocumentContentType,
  DocumentUploadUrlResponse,
  LocationCheckResult,
  MyVerification,
  VerificationCreateRequest,
  VerificationCreateResponse,
} from '../types/verification'
import { ApiError, apiFetch } from './client'

// 로그인·인증 플로우 v2(GPS 동네 인증 + 우선배정 서류) API.
//   POST /verifications/neighborhood          (본문 없음)                 → 동네 인증, 바로 APPROVED
//   POST /verifications/documents/upload-url  { fileName, contentType }   → 서류 업로드 URL + fileKey
//   POST /verifications                       { verificationType, documentType, fileKey, applicantName }
//                                                                         → 우선배정 인증 신청, PENDING
//   GET  /verifications/me                                                → 유형별 내 인증 상태

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

const DOCUMENT_CONTENT_TYPE_BY_EXTENSION: Record<string, DocumentContentType> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
  heic: 'image/heic',
  heif: 'image/heif',
  pdf: 'application/pdf',
}
const DOCUMENT_CONTENT_TYPES = new Set<string>(Object.values(DOCUMENT_CONTENT_TYPE_BY_EXTENSION))

/**
 * 서류 업로드 URL 발급에 쓸 contentType. 받지 않는 형식이면 null.
 * heic·heif는 브라우저에 따라 file.type이 빈 문자열이라 확장자로 보정한다.
 */
export function resolveDocumentContentType(file: File): DocumentContentType | null {
  if (DOCUMENT_CONTENT_TYPES.has(file.type)) return file.type as DocumentContentType
  if (file.type) return null
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  return DOCUMENT_CONTENT_TYPE_BY_EXTENSION[extension] ?? null
}

/**
 * 우선배정 서류 업로드. 업로드 URL을 발급받아(POST /verifications/documents/upload-url) S3에 바로 PUT 하고,
 * 인증 신청 때 담을 fileKey를 돌려준다. 서류는 공개 URL이 없다 (관리자만 열람).
 */
export async function uploadVerificationDocument(file: File, contentType: DocumentContentType): Promise<string> {
  const { uploadUrl, fileKey } = await apiFetch<DocumentUploadUrlResponse>('/verifications/documents/upload-url', {
    method: 'POST',
    body: JSON.stringify({ fileName: file.name, contentType }),
  })
  await putToS3(uploadUrl, file, contentType)
  return fileKey
}

/** 내 인증 현황 (GET /verifications/me). 신청한 유형만, 유형마다 가장 최근 건 기준으로 내려온다. */
export function getMyVerifications() {
  return apiFetch<MyVerification[]>('/verifications/me')
}
