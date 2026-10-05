import type { TradeMethod } from '../types/item'
import { apiFetch } from './client'

export type ReservationStatus =
  | 'SCHEDULED'
  | 'HUB_DROP_SCHEDULED'
  | 'HUB_RECEIVED'
  | 'PICKUP_SCHEDULED'
  | 'RECONFIRMED'
  | 'COMPLETED'
  | 'NO_SHOW'
  | 'CANCELED'

/** GET /applications/{applicationId}/reservation 응답 */
export interface ReservationDetail {
  id: number
  applicationId: number
  tradeMethod: TradeMethod
  scheduledAt: string | null
  status: ReservationStatus
  reconfirmationDeadline: string | null
  reconfirmedAt: string | null
  /**
   * 거래 상대 — 신청자가 보면 등록자, 등록자가 보면 신청자.
   * 상대가 공개하기로 고른 연락 수단만 값이 있고, 노쇼·취소된 예약은 둘 다 null이다.
   */
  counterpart: {
    nickname: string
    contactType: 'PHONE' | 'OPENCHAT' | null
    phone: string | null
    openchatLink: string | null
  }
}

/**
 * 예약 상세 (GET /applications/{applicationId}/reservation, 로그인 필요). 연락 수단이 공개되는 유일한 API.
 * 아직 배정 전이라 예약이 없으면 404 RESERVATION_NOT_FOUND, 신청자도 등록자도 아니면 403.
 */
export function getReservation(applicationId: number) {
  return apiFetch<ReservationDetail>(`/applications/${applicationId}/reservation`)
}

/** PATCH /reservations/{reservationId}/reconfirm 응답 */
export interface ReconfirmResponse {
  id: number
  status: ReservationStatus
  reconfirmedAt: string
}

/** 진행 중이라 아직 수령 재확인을 할 수 있는 예약 상태 (백엔드 RECONFIRMABLE_STATUSES) */
export const RECONFIRMABLE_STATUSES: readonly ReservationStatus[] = [
  'SCHEDULED',
  'HUB_DROP_SCHEDULED',
  'HUB_RECEIVED',
  'PICKUP_SCHEDULED',
]

/**
 * 수령 재확인 (PATCH /reservations/{reservationId}/reconfirm). 배정된 신청자 본인만 할 수 있다.
 * 이미 했거나(RESERVATION_ALREADY_RECONFIRMED), 진행 중인 예약이 아니거나(RESERVATION_NOT_RECONFIRMABLE),
 * 기한이 지났으면(RESERVATION_RECONFIRMATION_EXPIRED) 409.
 */
export function reconfirmReservation(reservationId: number) {
  return apiFetch<ReconfirmResponse>(`/reservations/${reservationId}/reconfirm`, { method: 'PATCH' })
}
