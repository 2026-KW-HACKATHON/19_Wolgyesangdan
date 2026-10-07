import type {
  CategoryCarbon,
  ImageUploadUrlResponse,
  ItemCreateRequest,
  ItemDetail,
  ItemImageContentType,
  ItemSearchParams,
  ItemSummary,
  MyItemSummary,
  PageResponse,
} from '../types/item'
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

/** 물품 등록 (POST /items, 로그인 필요). 응답은 상세 조회와 같은 형태 */
export function createItem(request: ItemCreateRequest) {
  return apiFetch<ItemDetail>('/items', { method: 'POST', body: JSON.stringify(request) })
}

const CONTENT_TYPE_BY_EXTENSION: Record<string, ItemImageContentType> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
  heic: 'image/heic',
  heif: 'image/heif',
}
const ITEM_IMAGE_CONTENT_TYPES = new Set<string>(Object.values(CONTENT_TYPE_BY_EXTENSION))

/**
 * 업로드 URL 발급에 쓸 contentType. 받지 않는 형식이면 null.
 * heic·heif는 브라우저에 따라 file.type이 빈 문자열이라 확장자로 보정한다.
 */
export function resolveItemImageContentType(file: File): ItemImageContentType | null {
  if (ITEM_IMAGE_CONTENT_TYPES.has(file.type)) return file.type as ItemImageContentType
  if (file.type) return null
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  return CONTENT_TYPE_BY_EXTENSION[extension] ?? null
}

/**
 * 물품 사진 업로드. 업로드 URL을 발급받아(POST /items/images/upload-url) S3에 바로 PUT 하고,
 * 물품 등록 때 imageUrls에 담을 URL을 돌려준다.
 * Content-Type이 서명에 포함되므로 발급 요청과 같은 값으로 PUT 해야 한다 (다르면 S3가 403).
 */
export async function uploadItemImage(file: File, contentType: ItemImageContentType): Promise<string> {
  const { uploadUrl, imageUrl } = await apiFetch<ImageUploadUrlResponse>('/items/images/upload-url', {
    method: 'POST',
    body: JSON.stringify({ fileName: file.name, contentType }),
  })
  // S3로 바로 보내는 요청이라 apiFetch(우리 서버 주소·Authorization 헤더)를 쓰지 않는다
  const response = await fetch(uploadUrl, { method: 'PUT', headers: { 'Content-Type': contentType }, body: file })
  if (!response.ok) throw new Error(`S3 업로드 실패 (${response.status})`)
  return imageUrl
}
