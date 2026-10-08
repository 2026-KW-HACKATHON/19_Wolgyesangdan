import type { PageResponse } from '../types/page'
import type {
  AdminVerificationDetail,
  AdminVerificationFile,
  AdminVerificationSummary,
  VerificationStatus,
} from '../types/verification'
import { apiFetch } from './client'

/** 서류 목록 (GET /admin/verifications). 최근 신청순, status를 안 주면 전부 */
export function getAdminVerifications(status?: VerificationStatus, page = 0) {
  const query = new URLSearchParams({ page: String(page) })
  if (status) query.set('status', status)
  return apiFetch<PageResponse<AdminVerificationSummary>>(`/admin/verifications?${query}`)
}

export function getAdminVerification(verificationId: number) {
  return apiFetch<AdminVerificationDetail>(`/admin/verifications/${verificationId}`)
}

/** 서류 열람 주소 (5분 유효). 부를 때마다 서버에 열람 기록이 남는다 */
export function getAdminVerificationFile(verificationId: number) {
  return apiFetch<AdminVerificationFile>(`/admin/verifications/${verificationId}/file`)
}

/** 승인. 신입생은 서류에서 확인한 입학 연도가 필요하다 (그해 12월 31일까지 유효). 기초수급자는 승인일부터 1년 */
export function approveAdminVerification(verificationId: number, admissionYear?: number) {
  return apiFetch<AdminVerificationDetail>(`/admin/verifications/${verificationId}/approve`, {
    method: 'POST',
    ...(admissionYear !== undefined ? { body: JSON.stringify({ admissionYear }) } : {}),
  })
}

/** 반려. 사유는 회원 앱 인증 화면에 그대로 보인다 */
export function rejectAdminVerification(verificationId: number, reason: string) {
  return apiFetch<AdminVerificationDetail>(`/admin/verifications/${verificationId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  })
}
