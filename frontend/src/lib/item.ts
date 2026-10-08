import type { CategoryGroup, ItemStatus, TradeMethod } from '../types/item'

/** 화면에 보여주는 물품 상태. CLOSED는 신청이 마감돼 배정을 기다리는 상태다 */
export const ITEM_STATUS_LABEL: Record<ItemStatus, string> = {
  REGISTERED: '등록됨',
  OPEN: '신청 가능',
  CLOSED: '배정 중',
  ASSIGNED: '배정 완료',
  COMPLETED: '거래 완료',
  CANCELED: '취소됨',
}

/** 상태 배지 색 — 신청 가능은 강조, 배정 진행 중은 주황, 나머지는 회색 */
export function itemStatusTone(status: ItemStatus): 'primary' | 'warning' | 'neutral' {
  if (status === 'OPEN') return 'primary'
  if (status === 'CLOSED' || status === 'ASSIGNED') return 'warning'
  return 'neutral'
}

/**
 * 등록자가 수정·삭제할 수 있는 물품인지 — 신청을 받는 중이고 아직 아무도 신청하지 않았을 때만.
 * 서버(Item.isModifiable)와 같은 기준이다
 */
export function isItemModifiable(item: { status: ItemStatus; applicantCount: number }) {
  return item.status === 'OPEN' && item.applicantCount === 0
}

export const TRADE_METHOD_LABEL: Record<TradeMethod, string> = {
  DIRECT: '직거래',
  CAMPAIGN: '거점 수령',
}

/** 사진이 없을 때 대신 보여주는 카테고리 아이콘 */
export const CATEGORY_ICON: Record<CategoryGroup, string> = {
  가구: 'chair',
  가전: 'bolt',
  주방: 'local_cafe',
  생활: 'home',
  기타: 'more_horiz',
}

/** 신청 마감 시각을 "10.2까지"로. 마감이 없으면 빈 문자열 */
export function formatDeadline(applicationDeadline: string | null) {
  if (!applicationDeadline) return ''
  const date = new Date(applicationDeadline)
  return `${date.getMonth() + 1}.${date.getDate()}까지`
}
