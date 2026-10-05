import type { PageResponse } from '../types/item'
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

/** GET /users/me/applications 의 카드 한 장 */
export interface MyApplicationSummary {
  /** 신청 id — 예약 상세·신청 취소에 쓴다 */
  id: number
  itemId: number
  itemName: string
  itemThumbnailImageUrl: string | null
  status: ApplicationStatus
  /** 대기 순번. 배정된 뒤에도 그대로 남고, 취소하면 null */
  waitlistRank: number | null
  appliedAt: string
}

/** 내가 신청한 물품 (GET /users/me/applications, 로그인 필요). 최근 신청순 */
export function getMyApplications(page = 0, size = 20) {
  return apiFetch<PageResponse<MyApplicationSummary>>(`/users/me/applications?page=${page}&size=${size}`)
}
