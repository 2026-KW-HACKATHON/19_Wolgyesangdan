import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { kakaoLogin } from '../api/auth'
import { ApiError } from '../api/client'
import MaterialIcon from '../components/icons/MaterialIcon'
import PrimaryButton from '../components/PrimaryButton'
import Screen from '../components/Screen'
import { isLoggedIn, saveTokens } from '../lib/authStorage'
import { clearKakaoState, isValidKakaoState } from '../lib/kakao'
import { leaveKakaoCallback, loginPath, takeLoginNext } from '../lib/loginRedirect'

/**
 * 카카오 로그인 후 돌아오는 화면 (/oauth/kakao/callback).
 * 인가 코드를 백엔드(POST /auth/kakao)에 넘겨 서비스 토큰을 받고,
 * 신규 회원은 GPS 동네 인증 화면으로, 기존 회원은 홈으로 보낸다.
 */
export default function KakaoCallbackPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [loginError, setLoginError] = useState<string | null>(null)
  const requested = useRef(false)

  const code = searchParams.get('code')
  // 사용자가 카카오 동의 화면에서 취소한 경우 ?error=access_denied 로 돌아온다
  const cancelled = searchParams.get('error') !== null
  const invalidRequest = !cancelled && (!code || !isValidKakaoState(searchParams.get('state')))
  const errorMessage = invalidRequest ? '잘못된 로그인 요청이에요. 다시 시도해주세요.' : loginError
  // 이미 로그인했는데 쓰고 난 콜백 주소로 다시 온 경우(뒤로가기로 카카오 페이지를 거쳐 돌아옴) — 오류 대신 그 전 화면으로 (#186)
  const returnedAfterLogin = isLoggedIn() && (cancelled || invalidRequest)

  useEffect(() => {
    // 인가 코드는 한 번만 쓸 수 있다 — 개발 모드(StrictMode)에서 effect가 두 번 돌아도 요청은 한 번만.
    if (requested.current) return
    requested.current = true

    if (returnedAfterLogin) {
      leaveKakaoCallback(navigate)
      return
    }

    if (cancelled) {
      // 동의를 취소하고 다시 로그인 화면으로 — 돌아갈 화면은 이어서 넘긴다
      navigate(loginPath(takeLoginNext() ?? ''), { replace: true })
      return
    }
    if (invalidRequest || !code) return

    kakaoLogin(code)
      .then((response) => {
        clearKakaoState()
        saveTokens(response)
        // 기존 회원은 원래 보던 화면으로. 신규 회원은 동네 인증부터 하고, 돌아갈 화면은 인증을 건너뛸 때 쓴다
        navigate(response.isNewUser ? '/verify/location' : (takeLoginNext() ?? '/'), { replace: true })
      })
      .catch((error: unknown) => {
        setLoginError(
          error instanceof ApiError ? error.message : '로그인 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.',
        )
      })
  }, [cancelled, code, invalidRequest, navigate, returnedAfterLogin])

  if (returnedAfterLogin) return null

  return (
    <Screen>
      <div className="flex flex-1 flex-col items-center justify-center px-8 text-center">
        {errorMessage ? (
          <>
            <span className="flex size-16 items-center justify-center rounded-full bg-primary-tint text-accent">
              <MaterialIcon name="error" size={32} />
            </span>
            <p className="mt-3 text-[15px] leading-[1.6] font-medium text-ink-3">{errorMessage}</p>
            <PrimaryButton
              label="로그인 화면으로"
              onClick={() => navigate(loginPath(takeLoginNext() ?? ''), { replace: true })}
              className="mt-6"
            />
          </>
        ) : (
          <p className="text-[15px] font-medium text-ink-3">로그인 중이에요…</p>
        )}
      </div>
    </Screen>
  )
}
