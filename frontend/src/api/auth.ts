import type { LoginResponse } from '../types/auth'
import { apiFetch } from './client'

export function kakaoLogin(authorizationCode: string) {
  return apiFetch<LoginResponse>('/auth/kakao', {
    method: 'POST',
    body: JSON.stringify({ authorizationCode }),
  })
}
