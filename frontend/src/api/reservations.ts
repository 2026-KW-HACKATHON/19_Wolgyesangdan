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
   * 상대가 공개하기로 고른 연락 수단만 값이 있고, 노쇼·취소되거나 전달이 완료된 예약은 둘 다 null이다.
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

/** PATCH /reservations/{reservationId}/complete 응답 */
export interface CompleteResponse {
  id: number
  status: ReservationStatus
  completedAt: string
}

/**
 * 직거래 전달 완료 (PATCH /reservations/{reservationId}/complete). 물품 등록자만 할 수 있다.
 * 신청자가 수령을 재확인한 직거래 예약만 완료된다 — 재확인 전이면 RESERVATION_NOT_RECONFIRMED,
 * 거점 거래면 RESERVATION_NOT_DIRECT, 이미 끝났으면 RESERVATION_ALREADY_COMPLETED (모두 409).
 */
export function completeReservation(reservationId: number) {
  return apiFetch<CompleteResponse>(`/reservations/${reservationId}/complete`, { method: 'PATCH' })
}

/** GET /users/me/todo 응답 — 신청자로서 수령 재확인할 것, 등록자로서 전달할 것 */
export interface MyTodo {
  /** 기한 빠른 순 (기한 없는 건 맨 뒤) */
  reconfirms: {
    applicationId: number
    reservationId: number
    itemId: number
    itemName: string
    reconfirmationDeadline: string | null
  }[]
  /** 배정된 순 */
  deliveries: {
    itemId: number
    itemName: string
    reservationId: number
    applicationId: number
    tradeMethod: TradeMethod
    status: ReservationStatus
    reconfirmationDeadline: string | null
    /** 신청자가 수령 재확인했는지 */
    reconfirmed: boolean
  }[]
}

/** 내가 지금 해야 할 일 (GET /users/me/todo, 로그인 필요) — 홈 배너·탭 빨간 점용 */
export function getMyTodo() {
  return apiFetch<MyTodo>('/users/me/todo')
}
