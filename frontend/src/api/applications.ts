import type { PageResponse } from '../types/item'
import { apiFetch } from './client'

export type ApplicationStatus = 'WAITING' | 'SELECTED' | 'CANCELED' | 'COMPLETED'

/** POST /items/{itemId}/applications 응답 */
export interface ApplicationCreateResponse {
  id: number
  itemId: number
  status: ApplicationStatus
  /** 대기 순번. 배정 전에는 알려주지 않아서 신청 직후에는 항상 null (#265) */
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
  /** 대기 순번 (1부터). 물품이 아직 배정 전이면 null (#265). 배정된 뒤에는 그대로 남고, 취소하면 null */
  waitlistRank: number | null
  appliedAt: string
}

/** 내가 신청한 물품 (GET /users/me/applications, 로그인 필요). 최근 신청순 */
export function getMyApplications(page = 0, size = 20) {
  return apiFetch<PageResponse<MyApplicationSummary>>(`/users/me/applications?page=${page}&size=${size}`)
}

/**
 * 신청 취소 (DELETE /applications/{applicationId}, 로그인 필요). 성공하면 204.
 * 대기 중(WAITING)인 신청만 취소할 수 있다 — 이미 배정됐으면 409 APPLICATION_ALREADY_SELECTED,
 * 본인 신청이 아니면 403 APPLICATION_NOT_OWNER, 없는 신청이면 404 APPLICATION_NOT_FOUND.
 */
export function cancelApplication(applicationId: number) {
  return apiFetch<void>(`/applications/${applicationId}`, { method: 'DELETE' })
}
