import type { AdminItem } from '../types/item'
import type { PageResponse } from '../types/page'
import { apiFetch } from './client'

/** 물품 목록 (GET /admin/items). 상태와 관계없이 전부, 최근 등록순. hidden을 안 주면 숨긴 물품까지 전부 */
export function getAdminItems(hidden?: boolean, page = 0) {
  const query = new URLSearchParams({ page: String(page) })
  if (hidden !== undefined) query.set('hidden', String(hidden))
  return apiFetch<PageResponse<AdminItem>>(`/admin/items?${query}`)
}

/** 숨기기 / 다시 보이기 (PATCH /admin/items/{id}). 바뀐 물품을 돌려준다 */
export function setAdminItemHidden(itemId: number, hidden: boolean) {
  return apiFetch<AdminItem>(`/admin/items/${itemId}`, {
    method: 'PATCH',
    body: JSON.stringify({ hidden }),
  })
}
