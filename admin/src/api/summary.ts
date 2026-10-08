import type { PageResponse } from '../types/page'
import type { AdminSummary, RecentVerification } from '../types/summary'
import { apiFetch } from './client'

/** 대시보드 요약 (GET /admin/summary) — 처리할 일 건수와 현재 캠페인 */
export function getAdminSummary() {
  return apiFetch<AdminSummary>('/admin/summary')
}

/** 최근 들어온 서류 (GET /admin/verifications, 최근 신청순) — 별도 API 없이 목록을 size만 줄여 부른다 */
export function getRecentVerifications(size = 3) {
  return apiFetch<PageResponse<RecentVerification>>(`/admin/verifications?page=0&size=${size}`)
}
