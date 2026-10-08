import type { MyInfo } from '../types/user'
import { apiFetch } from './client'

/** 내 정보 (GET /users/me). role로 운영자인지 확인한다 */
export function getMyInfo() {
  return apiFetch<MyInfo>('/users/me')
}
