import type { LoginResponse } from '../types/auth'
import { apiFetch } from './client'

/** 관리자 로그인 (POST /auth/admin/login). 틀리면 401 AUTH_INVALID_ADMIN_CREDENTIALS */
export function adminLogin(loginId: string, password: string) {
  return apiFetch<LoginResponse>('/auth/admin/login', {
    method: 'POST',
    body: JSON.stringify({ loginId, password }),
  })
}
