import type { AuthTokens } from '../types/auth'

const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'

// 로그아웃 API는 MVP에서 만들지 않음 — 로그아웃은 clearTokens()로 로컬 토큰만 지우면 된다.

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function saveTokens({ accessToken, refreshToken }: AuthTokens) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function isLoggedIn() {
  return getAccessToken() !== null
}

/** 로그인이 풀렸을 때(재발급 실패, 무효한 토큰, 회원 없음) 화면에 알리는 이벤트 — SessionExpiredDialog가 듣는다 */
export const SESSION_EXPIRED_EVENT = 'auth:session-expired'

/** 더 쓸 수 없는 로그인을 정리한다 — 토큰을 지우고 화면에 알린다 (#171) */
export function expireSession() {
  clearTokens()
  window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
}
