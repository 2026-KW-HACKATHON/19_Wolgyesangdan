import MarketLogo from '../assets/MarketLogo'
import MaterialIcon from '../components/icons/MaterialIcon'
import Screen from '../components/Screen'
import type { SocialProvider } from '../types/auth'

interface LoginPageProps {
  /** 지정하지 않으면 OAuth 리다이렉트 URL로 이동하는 기본 동작을 씁니다. */
  onLogin?: (provider: SocialProvider) => void
  onBrowse?: () => void
}

/**
 * 로그인 화면 (#1a) — design_handoff_login_auth/README.md 기준 구현.
 *
 * 로그인 없이도 "둘러보기"로 홈에 진입할 수 있고, 신청·등록 시점에만
 * 로그인이 요구됩니다. 소셜 로그인은 OAuth 리다이렉트로 처리됩니다.
 */
export default function LoginPage({ onLogin, onBrowse }: LoginPageProps) {
  const handleKakaoLogin = () => {
    if (onLogin) return onLogin('kakao')
    // TODO: 카카오 OAuth 연동 — 백엔드 엔드포인트로 리다이렉트
    window.location.href = '/auth/social/kakao'
  }

  const handleGoogleLogin = () => {
    if (onLogin) return onLogin('google')
    // TODO: 구글 OAuth 연동 — 백엔드 엔드포인트로 리다이렉트
    window.location.href = '/auth/social/google'
  }

  const handleBrowse = () => {
    if (onBrowse) return onBrowse()
    // TODO: 라우터 도입 후 비로그인 상태로 홈으로 이동하도록 교체
    window.location.href = '/'
  }

  return (
    <Screen>
      <header className="flex h-14 items-center justify-end px-5">
        <button
          type="button"
          onClick={handleBrowse}
          className="cursor-pointer text-[14px] font-semibold text-label-alt"
        >
          둘러보기
        </button>
      </header>

      <main className="flex flex-1 flex-col items-center justify-center px-8 text-center">
        <MarketLogo />
        <h1 className="mt-2.5 font-hand text-[44px] leading-[1.1] font-bold text-accent">월계상단</h1>
        <p className="mt-0.5 text-[12px] font-bold tracking-[0.3em] text-amber-ink">
          월계1동 나눔장터
        </p>
        <p className="mt-2.5 text-[15px] leading-[1.6] font-medium text-ink-3">
          버려질 뻔한 물건을 월계1동 안에서
          <br />
          다시 쓰도록 잇습니다
        </p>
      </main>

      <div className="flex flex-col gap-2.5 px-6 pb-2">
        <button
          type="button"
          onClick={handleKakaoLogin}
          className="relative flex h-[54px] cursor-pointer items-center justify-center gap-2 rounded-[14px] bg-kakao text-[16px] font-bold text-kakao-ink"
        >
          <MaterialIcon name="chat_bubble" size={20} className="absolute left-[18px]" />
          카카오로 시작하기
        </button>
        <button
          type="button"
          onClick={handleGoogleLogin}
          className="relative flex h-[54px] cursor-pointer items-center justify-center gap-2 rounded-[14px] border border-border-strong bg-white text-[16px] font-bold text-google-ink"
        >
          <span
            aria-hidden="true"
            className="absolute left-[18px] flex size-5 items-center justify-center rounded-full bg-google-badge text-[13px] font-extrabold text-ink-2"
          >
            G
          </span>
          구글로 시작하기
        </button>
      </div>

      <p className="px-8 pt-3.5 pb-7 text-center text-[12px] leading-[1.6] font-medium text-label-alt">
        시작하면{' '}
        <a href="/terms" className="font-bold text-accent hover:text-primary-dark">
          이용약관
        </a>
        과{' '}
        <a href="/privacy" className="font-bold text-accent hover:text-primary-dark">
          개인정보 처리방침
        </a>
        에
        <br />
        동의한 것으로 봅니다
      </p>
    </Screen>
  )
}
