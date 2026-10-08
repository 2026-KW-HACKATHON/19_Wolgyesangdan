/** 백엔드 목록 API 공통 페이지 응답 (global/dto/PageResponse) */
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
