import type { CategoryCarbon, ItemDetail, ItemSearchParams, ItemSummary, MyItemSummary, PageResponse } from '../types/item'
import { apiFetch } from './client'

/** 물품 목록 (GET /items, 비회원 허용). 조건은 모두 선택이고, 비운 조건은 보내지 않는다. */
export function getItems(params: ItemSearchParams = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) query.set(key, String(value))
  }
  const queryString = query.toString()
  return apiFetch<PageResponse<ItemSummary>>(queryString ? `/items?${queryString}` : '/items')
}

/** 카테고리별 예상 탄소 절감량 (GET /items/categories, 비회원 허용). 등록 시 서버가 저장하는 값과 같은 참조표 */
export function getCategories() {
  return apiFetch<CategoryCarbon[]>('/items/categories')
}

/** 물품 상세 (GET /items/{itemId}, 비회원 허용). 없는 물품이면 404 ITEM_NOT_FOUND */
export function getItem(itemId: number | string) {
  return apiFetch<ItemDetail>(`/items/${encodeURIComponent(itemId)}`)
}

/** 내가 등록한 물품 (GET /users/me/items, 로그인 필요). 상태와 관계없이 전부, 최근 등록순 */
export function getMyItems(page = 0, size = 20) {
  return apiFetch<PageResponse<MyItemSummary>>(`/users/me/items?page=${page}&size=${size}`)
}
