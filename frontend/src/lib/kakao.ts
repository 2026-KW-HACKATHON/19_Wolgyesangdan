const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY
const KAKAO_REDIRECT_URI = import.meta.env.VITE_KAKAO_REDIRECT_URI
const STATE_KEY = 'kakaoOAuthState'

function getKakao() {
  const kakao = window.Kakao
  if (!kakao) throw new Error('카카오 SDK를 불러오지 못했어요. 새로고침 후 다시 시도해주세요.')
  if (!kakao.isInitialized()) kakao.init(KAKAO_JS_KEY)
  return kakao
}

/**
 * 카카오 로그인 페이지로 이동한다. 로그인이 끝나면 카카오가
 * VITE_KAKAO_REDIRECT_URI(/oauth/kakao/callback)로 ?code=...&state=... 를 붙여 돌려보낸다.
 *
 * state: 우리가 시작한 로그인의 콜백인지 확인하는 값 (CSRF 방지).
 */
export function startKakaoLogin() {
  const state = crypto.randomUUID()
  sessionStorage.setItem(STATE_KEY, state)
  getKakao().Auth.authorize({ redirectUri: KAKAO_REDIRECT_URI, state })
}

/** 콜백으로 돌아온 state가 로그인 시작 때 저장한 값과 같은지 확인한다. */
export function isValidKakaoState(state: string | null) {
  const saved = sessionStorage.getItem(STATE_KEY)
  return saved !== null && saved === state
}

export function clearKakaoState() {
  sessionStorage.removeItem(STATE_KEY)
}
