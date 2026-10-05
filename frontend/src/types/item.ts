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

export interface ItemSearchParams {
  categoryGroup?: CategoryGroup
  tradeMethod?: TradeMethod
  sort?: ItemSort
  page?: number
  size?: number
}
