export type ReservationStatus =
  | 'SCHEDULED'
  | 'HUB_DROP_SCHEDULED'
  | 'HUB_RECEIVED'
  | 'PICKUP_SCHEDULED'
  | 'RECONFIRMED'
  | 'COMPLETED'
  | 'NO_SHOW'
  | 'CANCELED'

/**
 * GET /admin/hub-trades 의 한 줄 (AdminHubTradeResponse) — 거점 거래 예약 하나. id는 예약 id.
 * 입고 여부는 status가 아니라 atHub로 본다 (신청자가 먼저 수령을 재확인하면 status는 RECONFIRMED가 된다).
 * 시각은 YYYY-MM-DDTHH:mm:ss
 */
export interface AdminHubTrade {
  id: number
  itemId: number
  itemName: string
  ownerNickname: string
  applicantNickname: string
  status: ReservationStatus
  atHub: boolean
  hubReceivedAt: string | null
  reconfirmedAt: string | null
  reconfirmationDeadline: string | null
  completedAt: string | null
  /** 이 신청자에게 배정된 시각 */
  assignedAt: string
}

/** 운영진이 하는 처리 — 입고 / 수령 완료 / 미수령 */
export type HubTradeAction = 'receive' | 'complete' | 'no-show'
