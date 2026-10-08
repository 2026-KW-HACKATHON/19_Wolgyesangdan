export type ItemStatus = 'REGISTERED' | 'OPEN' | 'CLOSED' | 'ASSIGNED' | 'COMPLETED' | 'CANCELED'

/** GET /admin/items 의 한 줄 (AdminItemResponse). createdAt은 YYYY-MM-DDTHH:mm:ss */
export interface AdminItem {
  id: number
  name: string
  ownerNickname: string
  /** 한글 표시값 (가구 · 가전 · 주방 · 생활 · 기타) */
  categoryGroup: string
  status: ItemStatus
  createdAt: string
  hidden: boolean
}
