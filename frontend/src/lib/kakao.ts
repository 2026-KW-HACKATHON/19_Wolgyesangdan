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
 * 화면만 모바일로 바꾼 PC인지 (Chrome 개발자 도구의 기기 툴바 등).
 * 기기 툴바는 UA를 안드로이드로 바꾸지만 navigator.platform은 실제 OS(Win32, MacIntel, Linux x86_64) 그대로다.
 * 실제 안드로이드는 "Linux armv8l" / "Linux aarch64", iPhone은 "iPhone"으로 나온다.
 */
function isDesktopPlatform() {
  const platform = navigator.platform ?? ''
  return /^(Win|Mac)/i.test(platform) || /Linux (x86|i\d86|amd64)/i.test(platform)
}

/**
 * 카카오 로그인 페이지로 이동한다. 로그인이 끝나면 카카오가
 * VITE_KAKAO_REDIRECT_URI(/oauth/kakao/callback)로 ?code=...&state=... 를 붙여 돌려보낸다.
 *
 * state: 우리가 시작한 로그인의 콜백인지 확인하는 값 (CSRF 방지).
 *
 * SDK는 UA가 안드로이드면 카카오톡 앱 로그인(intent: 주소)부터 시도한다. PC에는 카카오톡 앱이 없어
 * 기기 툴바를 켠 채로는 거기서 멈추므로(#185), PC에서는 앱 로그인을 끄고 웹 로그인으로 보낸다.
 * 실제 모바일 기기에서는 SDK 기본 동작(카카오톡 앱 로그인) 그대로다.
 */
export function startKakaoLogin() {
  const state = crypto.randomUUID()
  sessionStorage.setItem(STATE_KEY, state)
  getKakao().Auth.authorize({
    redirectUri: KAKAO_REDIRECT_URI,
    state,
    ...(isDesktopPlatform() ? { throughTalk: false } : {}),
  })
}

/** 콜백으로 돌아온 state가 로그인 시작 때 저장한 값과 같은지 확인한다. */
export function isValidKakaoState(state: string | null) {
  const saved = sessionStorage.getItem(STATE_KEY)
  return saved !== null && saved === state
}

export function clearKakaoState() {
  sessionStorage.removeItem(STATE_KEY)
}
