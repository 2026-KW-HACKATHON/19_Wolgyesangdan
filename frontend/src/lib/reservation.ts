import type { ReservationStatus } from '../api/reservations'

/** 화면에 보여주는 예약 진행 상태 */
export const RESERVATION_STATUS_LABEL: Record<ReservationStatus, string> = {
  SCHEDULED: '약속 예정',
  HUB_DROP_SCHEDULED: '거점 입고 예정',
  HUB_RECEIVED: '거점 보관 중',
  PICKUP_SCHEDULED: '수령 예정',
  RECONFIRMED: '수령 확정',
  COMPLETED: '전달 완료',
  NO_SHOW: '미수령',
  CANCELED: '거래 취소',
}

/** "9.24" */
export function formatMonthDay(isoDateTime: string) {
  const date = new Date(isoDateTime)
  return `${date.getMonth() + 1}.${date.getDate()}`
}

/** "9.24 17:00" */
export function formatDateTime(isoDateTime: string) {
  const date = new Date(isoDateTime)
  return `${formatMonthDay(isoDateTime)} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}
