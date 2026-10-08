/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_KAKAO_JS_KEY: string
  readonly VITE_KAKAO_REDIRECT_URI: string
  /** 동네 인증 위치 — 'gps'면 실제 GPS, 그 외(비움 포함)는 월계1동 안의 고정 좌표 (#280, #288) */
  readonly VITE_LOCATION_MODE?: 'gps' | 'demo'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

/** index.html에서 로드하는 카카오 JS SDK v2 중 쓰는 부분만 선언 */
interface KakaoSdk {
  init: (appKey: string) => void
  isInitialized: () => boolean
  Auth: {
    /** throughTalk: 모바일에서 카카오톡 앱으로 간편 로그인을 시도할지 (기본 true) */
    authorize: (settings: { redirectUri: string; state?: string; throughTalk?: boolean }) => void
  }
}

interface Window {
  Kakao?: KakaoSdk
}
