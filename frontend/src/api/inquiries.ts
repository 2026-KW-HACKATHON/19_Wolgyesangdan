import type { Inquiry, InquiryCategory } from '../types/inquiry'
import type { PageResponse } from '../types/item'
import { apiFetch } from './client'

/** 문의 작성 (POST /inquiries, 로그인 필요). 제목 100자·내용 2000자까지 */
export function createInquiry(request: { category: InquiryCategory; title: string; content: string }) {
  return apiFetch<Inquiry>('/inquiries', { method: 'POST', body: JSON.stringify(request) })
}

/** 내 문의 (GET /users/me/inquiries, 로그인 필요). 답변까지 함께, 최근에 쓴 순 */
export function getMyInquiries(page = 0, size = 20) {
  return apiFetch<PageResponse<Inquiry>>(`/users/me/inquiries?page=${page}&size=${size}`)
}
