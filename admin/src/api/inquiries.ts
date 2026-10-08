import type { AdminInquiryDetail, AdminInquirySummary, InquiryStatus } from '../types/inquiry'
import type { PageResponse } from '../types/page'
import { apiFetch } from './client'

/** 문의 목록 (GET /admin/inquiries). 답변 대기가 먼저, 그 안에서는 최근에 들어온 순. status를 안 주면 전체 */
export function getAdminInquiries(status?: InquiryStatus, page = 0, size = 20) {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  if (status) query.set('status', status)
  return apiFetch<PageResponse<AdminInquirySummary>>(`/admin/inquiries?${query}`)
}

/** 문의 상세 (GET /admin/inquiries/{id}) — 본문과 답변 */
export function getAdminInquiry(inquiryId: number) {
  return apiFetch<AdminInquiryDetail>(`/admin/inquiries/${inquiryId}`)
}

/** 답변 등록 (POST /admin/inquiries/{id}/answer). 이미 답변했으면 409 INQUIRY_ALREADY_ANSWERED */
export function answerAdminInquiry(inquiryId: number, answer: string) {
  return apiFetch<AdminInquiryDetail>(`/admin/inquiries/${inquiryId}/answer`, {
    method: 'POST',
    body: JSON.stringify({ answer }),
  })
}
