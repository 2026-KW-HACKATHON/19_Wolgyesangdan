import type { ItemDetail, ItemSearchParams, ItemSummary, PageResponse } from '../types/item'
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

/** 물품 상세 (GET /items/{itemId}, 비회원 허용). 없는 물품이면 404 ITEM_NOT_FOUND */
export function getItem(itemId: number | string) {
  return apiFetch<ItemDetail>(`/items/${encodeURIComponent(itemId)}`)
}
