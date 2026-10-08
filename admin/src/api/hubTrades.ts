import type { AdminHubTrade, HubTradeAction } from '../types/hubTrade'
import type { PageResponse } from '../types/page'
import { apiFetch } from './client'

/** 거점 거래 목록 (GET /admin/hub-trades), 최근 배정순. done=false 진행 중 / true 끝난 거래 / 안 주면 전체 */
export function getAdminHubTrades(done?: boolean, page = 0) {
  const query = new URLSearchParams({ page: String(page) })
  if (done !== undefined) query.set('done', String(done))
  return apiFetch<PageResponse<AdminHubTrade>>(`/admin/hub-trades?${query}`)
}

/**
 * 거점 거래 처리 (POST /admin/hub-trades/{id}/receive | complete | no-show). 바뀐 예약을 돌려준다.
 * 수령 완료·미수령은 입고된 뒤에만 되고(409 RESERVATION_NOT_AT_HUB), 끝난 예약은 409 RESERVATION_ALREADY_CLOSED
 */
export function processAdminHubTrade(reservationId: number, action: HubTradeAction) {
  return apiFetch<AdminHubTrade>(`/admin/hub-trades/${reservationId}/${action}`, { method: 'POST' })
}
