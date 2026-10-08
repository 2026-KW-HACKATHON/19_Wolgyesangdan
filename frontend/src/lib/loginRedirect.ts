import type { NavigateFunction } from 'react-router-dom'

// 로그인 후 원래 보던 화면으로 돌아가기 (#172).
// 로그인 화면에 ?next=/items/123 처럼 돌아갈 경로를 넘기고, 카카오 로그인 페이지를 다녀오는 동안은
// sessionStorage에 보관한다 (카카오 state와 같은 방식).

const NEXT_KEY = 'loginNext'

/**
 * 우리 앱 안의 경로만 허용한다 — 외부 주소로 보내는 오픈 리다이렉트 방지.
 * "//evil.com"·"/\evil.com"은 브라우저가 다른 사이트로 해석하므로 막고, 로그인 흐름 자체로는 돌아가지 않는다.
 */
function isSafeNext(path: string | null): path is string {
  return (
    path !== null &&
    path.startsWith('/') &&
    !path.startsWith('//') &&
    !path.startsWith('/\\') &&
    !path.startsWith('/login') &&
    !path.startsWith('/oauth')
  )
}

/** 로그인 화면 주소. 돌아갈 경로가 안전하지 않으면 붙이지 않는다 */
export function loginPath(next: string) {
  return isSafeNext(next) ? `/login?next=${encodeURIComponent(next)}` : '/login'
}

/** 카카오 로그인을 시작할 때 돌아갈 경로를 보관한다. 없으면 이전 값을 지운다 */
export function rememberLoginNext(next: string | null) {
  if (isSafeNext(next)) sessionStorage.setItem(NEXT_KEY, next)
  else sessionStorage.removeItem(NEXT_KEY)
}

/** 보관한 경로를 꺼내고 지운다 — 한 번만 쓴다 */
export function takeLoginNext(): string | null {
  const next = sessionStorage.getItem(NEXT_KEY)
  sessionStorage.removeItem(NEXT_KEY)
  return isSafeNext(next) ? next : null
}

/**
 * 이미 로그인했는데 로그인 흐름 화면(로그인·카카오 콜백)에 다시 들어온 경우 — 로그인 후 돌아간 화면에서
 * 뒤로가기한 것이므로 한 칸 더 뒤로 보내 로그인 전에 보던 화면으로 간다 (#186). 뒤로 갈 기록이 없으면 홈.
 * React Router가 history.state.idx에 이 앱 안에서의 위치(0부터)를 기록한다.
 */
export function leaveLoginFlow(navigate: NavigateFunction) {
  const idx = (window.history.state as { idx?: number } | null)?.idx ?? 0
  if (idx > 0) navigate(-1)
  else navigate('/', { replace: true })
}

/**
 * 카카오 콜백용 — 콜백 주소는 카카오에서 넘어온 새 페이지라 앱 안 기록 번호(idx)가 0부터 시작한다.
 * 그 앞에는 로그인 화면(또는 카카오 페이지)이 있으므로 브라우저 기록으로 한 칸 뒤로 간다.
 * 로그인 화면으로 돌아가면 그 화면이 다시 한 칸 더 뒤로 보낸다 (#186).
 */
export function leaveKakaoCallback(navigate: NavigateFunction) {
  if (window.history.length > 1) window.history.back()
  else navigate('/', { replace: true })
}
