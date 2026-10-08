import { expireSession, getAccessToken, getRefreshToken, saveTokens } from '../lib/authStorage'
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

// refresh 토큰은 한 번 쓰면 폐기되므로(서버가 회전) 같은 토큰으로 두 번 재발급하면 늦은 쪽이 실패한다.
// - 같은 탭 안: 진행 중인 재발급(refreshing)을 공유해 한 번만 보낸다
// - 탭 사이(localStorage 공유): Web Locks로 한 번에 한 탭만 재발급하고, 기다리는 동안 다른 탭이 이미 새로
//   받았으면 요청하지 않고 그 토큰을 쓴다. 실패해도 그 사이 다른 탭이 저장한 새 토큰은 지우지 않는다 (#170)
let refreshing: Promise<boolean> | null = null

const REFRESH_LOCK = 'wolgyesangdan-admin-token-refresh'

function withRefreshLock(task: () => Promise<boolean>): Promise<boolean> {
  // Web Locks를 지원하지 않는 브라우저는 탭 사이 순서 보장 없이 진행한다 (실패 시 새 토큰을 지우지 않는 처리는 그대로)
  return navigator.locks ? navigator.locks.request(REFRESH_LOCK, task) : task()
}

async function requestNewTokens(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    const response = await fetch(`${API_BASE_URL}/auth/token/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!response.ok) {
      // 보낸 토큰이 아직 저장돼 있을 때만 지운다 — 바뀌었으면 다른 탭이 새로 받은 것이니 그걸로 계속한다
      if (getRefreshToken() === refreshToken) expireSession()
      return getRefreshToken() !== null
    }
    saveTokens((await response.json()) as AuthTokens)
    return true
  } catch {
    return false
  }
}

/** expiredAccessToken: 401(만료)을 받은 요청에 실어 보낸 access 토큰 */
function refreshTokens(expiredAccessToken: string | null): Promise<boolean> {
  refreshing ??= withRefreshLock(async () => {
    // 잠금을 기다리는 동안 다른 탭이 이미 재발급했으면 그 토큰으로 다시 시도한다
    const current = getAccessToken()
    if (current !== null && current !== expiredAccessToken) return true
    return requestNewTokens()
  }).finally(() => {
    refreshing = null
  })
  return refreshing
}

/** 재발급으로 해결되지 않는 401 — 토큰을 지우고 다시 로그인하게 한다 (#171) */
const SESSION_INVALID_CODES = new Set(['AUTH_INVALID_TOKEN', 'AUTH_USER_NOT_FOUND'])

/**
 * 백엔드 API 호출. 로그인 상태면 Authorization 헤더를 자동으로 붙이고,
 * access 토큰이 만료(AUTH_TOKEN_EXPIRED)되면 재발급 후 한 번 재시도한다.
 * 토큰이 무효하거나(AUTH_INVALID_TOKEN) 회원이 없으면(AUTH_USER_NOT_FOUND) 로그인을 정리한다.
 * 실패 응답은 ApiError로 던진다.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const sentAccessToken = getAccessToken()
  const response = await request(path, init)

  if (response.status === 401) {
    const body = await response.clone().json().catch(() => null)
    if (body?.code === 'AUTH_TOKEN_EXPIRED' && (await refreshTokens(sentAccessToken))) {
      return parse<T>(await request(path, init))
    }
    // 보낸 토큰이 아직 그대로일 때만 — 그 사이 다시 로그인했거나 다른 탭이 새로 받았으면 그 로그인은 지우지 않는다
    if (SESSION_INVALID_CODES.has(body?.code) && sentAccessToken !== null && getAccessToken() === sentAccessToken) {
      expireSession()
    }
  }
  return parse<T>(response)
}
