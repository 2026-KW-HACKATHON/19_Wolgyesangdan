import { useState, type CSSProperties } from 'react'
import ExchangeScene from '../components/ExchangeScene'
import MaterialIcon from '../components/icons/MaterialIcon'
import Screen from '../components/Screen'
import { startKakaoLogin } from '../lib/kakao'

// 뒤로 흩날리는 잎 — 다시 쓰기로 키우는 동네 나무(탄소절감 리포트)와 같은 잎이다. duration·delay는 초
const DRIFTING_LEAVES = [
  { left: '10%', top: '6%', dx: 46, dy: 300, duration: 9, delay: 0, color: '#7E9A55' },
  { left: '28%', top: '0%', dx: -30, dy: 340, duration: 11, delay: 3.2, color: '#9FB878' },
  { left: '52%', top: '4%', dx: 38, dy: 320, duration: 10, delay: 6, color: '#E4A574' },
  { left: '70%', top: '2%', dx: -44, dy: 310, duration: 9.5, delay: 1.6, color: '#5E7F35' },
  { left: '86%', top: '8%', dx: -26, dy: 290, duration: 10.5, delay: 4.6, color: '#9FB878' },
  { left: '40%', top: '10%', dx: 22, dy: 280, duration: 8.5, delay: 7.4, color: '#7E9A55' },
]

/** 등장 순서대로 떠오르는 요소의 지연 시간 */
const enter = (delay: number): CSSProperties => ({ animationDelay: `${delay}s` })

const FADE_UP = 'animate-fade-up motion-reduce:animate-none'

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

      <main className="relative isolate flex flex-1 flex-col items-center justify-center overflow-hidden px-8 text-center">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10">
          {DRIFTING_LEAVES.map((leaf, i) => (
            <span
              key={i}
              // 시작 전(delay 동안)에는 안 보이게 opacity-0, 움직임 줄이기 설정이면 아예 그리지 않는다
              className="absolute h-2 w-3.5 animate-leaf-drift rounded-[8px_0_8px_0] opacity-0 motion-reduce:hidden"
              style={
                {
                  left: leaf.left,
                  top: leaf.top,
                  background: leaf.color,
                  animationDuration: `${leaf.duration}s`,
                  animationDelay: `${leaf.delay}s`,
                  '--leaf-dx': `${leaf.dx}px`,
                  '--leaf-dy': `${leaf.dy}px`,
                } as CSSProperties
              }
            />
          ))}
        </div>

        <ExchangeScene className="animate-logo-pop motion-reduce:animate-none" />
        <h1
          className={`mt-2.5 font-hand text-[44px] leading-[1.1] font-bold text-accent ${FADE_UP}`}
          style={enter(0.2)}
        >
          월계상단
        </h1>
        <p className={`mt-0.5 text-[12px] font-bold tracking-[0.3em] text-amber-ink ${FADE_UP}`} style={enter(0.35)}>
          월계1동 나눔장터
        </p>
        <p className={`mt-2.5 text-[15px] leading-[1.6] font-medium text-ink-3 ${FADE_UP}`} style={enter(0.5)}>
          버려질 뻔한 물건을 월계1동 안에서
          <br />
          다시 쓰도록 잇습니다
        </p>
      </main>

      <div className={`flex flex-col gap-2.5 px-6 pb-2 ${FADE_UP}`} style={enter(0.7)}>
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

      <p
        className={`px-8 pt-3.5 pb-7 text-center text-[12px] leading-[1.6] font-medium text-label-alt ${FADE_UP}`}
        style={enter(0.8)}
      >
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
