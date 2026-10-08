// 백엔드 /items API 타입

import type { ApplicationStatus } from '../api/applications'

/** 카테고리 대분류 — API가 한글 표시값 그대로 주고받는다 */
export type CategoryGroup = '가구' | '가전' | '주방' | '생활' | '기타'

export type TradeMethod = 'DIRECT' | 'CAMPAIGN'

/** GET /items/categories 의 한 줄 — 카테고리별 예상 탄소 절감량 참조값과 그 아래 품목 (#284) */
export interface CategoryCarbon {
  categoryGroup: CategoryGroup
  /** 품목을 고르지 않았을 때(목록에 없는 물건) 쓰는 대분류 값 */
  carbonReductionKg: number
  /** 대분류 값의 근거. 품목 기능 이전 서버면 없다 */
  basis?: string | null
  /** 대분류에 속한 품목 (등록 화면 표시 순서). 품목 기능 이전 서버면 없다 */
  itemTypes?: ItemTypeOption[]
}

/** 품목 하나 — 품목별 예상 탄소 절감량과 근거 */
export interface ItemTypeOption {
  /** 등록 요청에 보낼 값 (예: REFRIGERATOR) */
  itemType: string
  /** 화면 표시 이름 (예: 냉장고) */
  label: string
  carbonReductionKg: number
  /** 예: "2도어 300L급 55kg · UK DESNZ 2024 냉장·냉동 기기" */
  basis: string
}

/** 목록·상세에는 OPEN·CLOSED·ASSIGNED·COMPLETED만 내려온다 */
export type ItemStatus = 'REGISTERED' | 'OPEN' | 'CLOSED' | 'ASSIGNED' | 'COMPLETED' | 'CANCELED'

/** 정렬 기준. 같은 순위끼리는 최신 등록순 */
export type ItemSort = 'LATEST' | 'CARBON' | 'CONDITION'

/** 목록에 보여줄 상태 범위 — OPEN: 신청 가능만, ACTIVE(기본): 거래 끝난 것 숨김, ALL: 전부 (#190) */
export type ItemAvailability = 'OPEN' | 'ACTIVE' | 'ALL'

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
  /** 품목 (#284). 품목 없이 등록한 물품이면 null */
  itemType?: string | null
  /** 절감량 근거. 품목 없이 등록한 물품이면 null */
  carbonBasis?: string | null
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
  /** 보는 사람이 등록자인지 (비회원이면 false) */
  isMine: boolean
  /** 보는 사람의 신청. 신청한 적 없음·취소함·비회원·등록자면 null */
  myApplication: { id: number; status: ApplicationStatus; waitlistRank: number | null } | null
}

/** POST /items 요청. 필수는 사진·물품명·카테고리 대분류·상태 등급·거래 방식 */
export interface ItemCreateRequest {
  name: string
  categoryGroup: CategoryGroup
  /** 품목 (선택). 고르면 품목 값, 비우면 대분류 값으로 탄소 절감량을 계산한다 */
  itemType?: string
  conditionGrade: ConditionGrade
  description?: string
  usagePeriod?: string
  defectYn?: boolean
  defectDescription?: string
  size?: string
  transportDifficulty?: TransportDifficulty
  /** YYYY-MM-DD */
  availableFrom?: string
  availableUntil?: string
  disposalDeadline?: string
  tradeMethods: TradeMethod[]
  /** 거점 거래(CAMPAIGN)를 고르면 필수 */
  campaignId?: number
  /** 업로드 URL 발급 응답의 imageUrl들 (1~5장, 앞이 대표 사진) */
  imageUrls: string[]
}

/** 물품 사진으로 받는 형식 */
export type ItemImageContentType = 'image/jpeg' | 'image/png' | 'image/webp' | 'image/heic' | 'image/heif'

/** POST /items/images/upload-url 응답 */
export interface ImageUploadUrlResponse {
  /** S3 presigned PUT URL (5분 유효) */
  uploadUrl: string
  /** 업로드 후 저장될 공개 URL — 물품 등록 시 imageUrls에 담는다 */
  imageUrl: string
}

export interface ItemSearchParams {
  /** 물품명 부분 일치 (앞뒤 공백 무시) */
  keyword?: string
  categoryGroup?: CategoryGroup
  tradeMethod?: TradeMethod
  sort?: ItemSort
  availability?: ItemAvailability
  page?: number
  size?: number
}

/** GET /users/me/items 의 카드 한 장 (마이페이지 "내가 등록한 물품") */
export interface MyItemSummary {
  id: number
  name: string
  thumbnailImageUrl: string | null
  status: ItemStatus
  applicantCount: number
  /** 배정된 신청 id — 예약 상세 조회에 쓴다. 배정 확정 뒤(ASSIGNED·COMPLETED)에만 값이 있다 */
  applicationId: number | null
  /** 전달 예정 일시. 배정 확정 뒤에만 값이 있다 */
  scheduledAt: string | null
}
