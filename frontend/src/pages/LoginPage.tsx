import { useState } from 'react'
import MarketLogo from '../assets/MarketLogo'
import MaterialIcon from '../components/icons/MaterialIcon'
import Screen from '../components/Screen'
import { startKakaoLogin } from '../lib/kakao'

interface LoginPageProps {
  onBrowse?: () => void
}

/**
 * 로그인 화면 (#1a) — design_handoff_login_auth/README.md 기준 구현.
 *
 * 로그인 없이도 "둘러보기"로 홈에 진입할 수 있고, 신청·등록 시점에만
 * 로그인이 요구됩니다. 로그인은 카카오만 지원합니다 — 카카오 로그인 후
 * /oauth/kakao/callback(KakaoCallbackPage)으로 돌아옵니다.
 */
export default function LoginPage({ onBrowse }: LoginPageProps) {
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const handleKakaoLogin = () => {
    try {
      startKakaoLogin()
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '카카오 로그인을 시작하지 못했어요.')
    }
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
        {errorMessage && (
          <p role="alert" className="text-center text-[13px] font-medium text-ink-3">
            {errorMessage}
          </p>
        )}
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
