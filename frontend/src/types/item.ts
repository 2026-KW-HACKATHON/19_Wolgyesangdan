// 백엔드 /items API 타입

/** 카테고리 대분류 — API가 한글 표시값 그대로 주고받는다 */
export type CategoryGroup = '가구' | '가전' | '주방' | '생활' | '기타'

export type TradeMethod = 'DIRECT' | 'CAMPAIGN'

/** 목록·상세에는 OPEN·CLOSED·ASSIGNED·COMPLETED만 내려온다 */
export type ItemStatus = 'REGISTERED' | 'OPEN' | 'CLOSED' | 'ASSIGNED' | 'COMPLETED' | 'CANCELED'

/** 정렬 기준. 같은 순위끼리는 최신 등록순 */
export type ItemSort = 'LATEST' | 'CARBON' | 'CONDITION'

/** 목록 API 공통 페이지 응답 */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  /** 현재 페이지 (0부터) */
  number: number
  size: number
  first: boolean
  last: boolean
}

/** GET /items 의 카드 한 장 */
export interface ItemSummary {
  id: number
  name: string
  /** 세부 카테고리 (선택 입력) */
  category: string | null
  categoryGroup: CategoryGroup
  conditionGrade: string
  thumbnailImageUrl: string | null
  estimatedCarbonReduction: number
  tradeMethods: TradeMethod[]
  status: ItemStatus
  applicantCount: number
  maxApplicants: number
  applicationDeadline: string | null
}

/** 물품 상태 등급 (등록 시 셋 중 하나) */
export type ConditionGrade = '거의 새것' | '상태 좋음' | '사용감 있음'

export type TransportDifficulty = '쉬움' | '보통' | '어려움'

/** GET /items/{itemId}. 등록자의 연락처는 담기지 않는다 (배정된 상대에게만 예약 상세로 공개) */
export interface ItemDetail {
  id: number
  name: string
  category: string | null
  categoryGroup: CategoryGroup
  description: string | null
  conditionGrade: string
  usagePeriod: string | null
  defectYn: boolean
  defectDescription: string | null
  workingStatus: string | null
  size: string | null
  transportDifficulty: string | null
  estimatedCarbonReduction: number
  /** 전달 가능 기간 (YYYY-MM-DD) */
  availableFrom: string | null
  availableUntil: string | null
  disposalDeadline: string | null
  applicationDeadline: string | null
  status: ItemStatus
  applicantCount: number
  maxApplicants: number
  tradeMethods: TradeMethod[]
  images: { imageUrl: string; displayOrder: number }[]
  /** 거점 수령을 지원하는 물품만 값이 있다 */
  campaign: { id: number; locationName: string; locationAddress: string; hubHours: string | null } | null
  owner: { nickname: string; givenCount: number }
}

export interface ItemSearchParams {
  categoryGroup?: CategoryGroup
  tradeMethod?: TradeMethod
  sort?: ItemSort
  page?: number
  size?: number
}
