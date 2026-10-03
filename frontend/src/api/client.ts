import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../lib/authStorage'
import type { AuthTokens } from '../types/auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

/** 백엔드 ErrorResponse 형식 그대로 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly errors: { field: string; message: string }[]

  constructor(status: number, code: string, message: string, errors: { field: string; message: string }[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.errors = errors
  }
}

function request(path: string, init: RequestInit) {
  const headers = new Headers(init.headers)
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const accessToken = getAccessToken()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  return fetch(`${API_BASE_URL}${path}`, { ...init, headers })
}

async function parse<T>(response: Response): Promise<T> {
  if (response.ok) {
    return (response.status === 204 ? undefined : await response.json()) as T
  }
  const body = await response.json().catch(() => null)
  throw new ApiError(
    response.status,
    body?.code ?? 'UNKNOWN',
    body?.message ?? '요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.',
    body?.errors ?? [],
  )
}

// 여러 요청이 동시에 만료돼도 재발급은 한 번만 하도록 진행 중인 요청을 공유한다.
// (refresh 토큰은 한 번 쓰면 폐기되므로 두 번 보내면 두 번째는 실패함)
let refreshing: Promise<boolean> | null = null

function refreshTokens(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return Promise.resolve(false)

  refreshing ??= fetch(`${API_BASE_URL}/auth/token/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (response) => {
      if (!response.ok) {
        clearTokens()
        return false
      }
      saveTokens((await response.json()) as AuthTokens)
      return true
    })
    .catch(() => false)
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

/**
 * 백엔드 API 호출. 로그인 상태면 Authorization 헤더를 자동으로 붙이고,
 * access 토큰이 만료(AUTH_TOKEN_EXPIRED)되면 재발급 후 한 번 재시도한다.
 * 실패 응답은 ApiError로 던진다.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await request(path, init)

  if (response.status === 401) {
    const body = await response.clone().json().catch(() => null)
    if (body?.code === 'AUTH_TOKEN_EXPIRED' && (await refreshTokens())) {
      return parse<T>(await request(path, init))
    }
  }
  return parse<T>(response)
}
