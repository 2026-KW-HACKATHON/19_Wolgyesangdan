import { apiFetch } from './client'

export type ApplicationStatus = 'WAITING' | 'SELECTED' | 'CANCELED' | 'COMPLETED'

/** POST /items/{itemId}/applications 응답 */
export interface ApplicationCreateResponse {
  id: number
  itemId: number
  status: ApplicationStatus
  /** 대기 순번 (1부터) */
  waitlistRank: number | null
  appliedAt: string
}

/**
 * 물품 신청 (POST /items/{itemId}/applications, 로그인 필요).
 * 동네 인증이 없거나(APPLICATION_NOT_ELIGIBLE), 연락 수단이 없거나(APPLICATION_CONTACT_NOT_SET),
 * 본인 물품이면(APPLICATION_OWN_ITEM) 403, 이미 신청했거나 신청 가능한 상태가 아니면 409.
 */
export function applyForItem(itemId: number) {
  return apiFetch<ApplicationCreateResponse>(`/items/${itemId}/applications`, { method: 'POST' })
}
