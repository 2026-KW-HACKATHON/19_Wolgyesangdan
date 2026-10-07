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
