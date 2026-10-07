import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { SESSION_EXPIRED_EVENT } from '../lib/authStorage'
import { loginPath } from '../lib/loginRedirect'
import LoginRequiredDialog from './LoginRequiredDialog'

/**
 * 로그인이 풀리면(재발급 실패, 무효한 토큰, 회원 없음 — apiFetch가 expireSession 호출) "로그인이 필요해요" 팝업을 띄운다 (#171).
 * 비회원도 보는 화면에서도 백그라운드 요청(연락 수단 조회 등)이 실패할 수 있어서, 바로 로그인 화면으로 보내지 않고 고르게 한다.
 */
export default function SessionExpiredDialog() {
  const navigate = useNavigate()
  const location = useLocation()
  const [open, setOpen] = useState(false)

  useEffect(() => {
    const handleExpired = () => setOpen(true)
    window.addEventListener(SESSION_EXPIRED_EVENT, handleExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleExpired)
  }, [])

  // 로그인 흐름 안에서는 띄우지 않는다
  if (!open || location.pathname.startsWith('/login') || location.pathname.startsWith('/oauth')) return null

  const here = location.pathname + location.search
  return (
    <LoginRequiredDialog
      description="로그인 정보가 만료됐어요. 다시 로그인해 주세요."
      onLogin={() => {
        setOpen(false)
        navigate(loginPath(here))
      }}
      onCancel={() => {
        setOpen(false)
        // 같은 주소로 다시 이동해 화면이 비로그인 상태로 다시 그려지게 한다
        navigate(here, { replace: true })
      }}
    />
  )
}
