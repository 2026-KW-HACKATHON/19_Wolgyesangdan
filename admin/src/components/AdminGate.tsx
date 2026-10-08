import { useEffect, useState, type ReactNode } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getMyInfo } from '../api/users'
import AdminLayout from '../layouts/AdminLayout'
import { clearTokens, isLoggedIn, SESSION_EXPIRED_EVENT } from '../lib/authStorage'
import type { MyInfo } from '../types/user'
import MaterialIcon from './MaterialIcon'

type GateState = { status: 'loading' } | { status: 'ready'; me: MyInfo } | { status: 'error'; message: string }

function CenterMessage({ icon, message, action }: { icon: string; message: string; action?: ReactNode }) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
      <MaterialIcon name={icon} size={36} className="text-accent" />
      <p className="text-[15px] font-medium text-body">{message}</p>
      {action}
    </div>
  )
}

/**
 * 관리자 화면 입구. 로그인하지 않았으면 로그인 화면으로 보내고,
 * GET /users/me의 role이 ADMIN이 아니면 막는다 (백엔드도 /admin/** 를 ADMIN만 허용한다).
 */
export default function AdminGate() {
  const navigate = useNavigate()
  const loggedIn = isLoggedIn()
  const [state, setState] = useState<GateState>({ status: 'loading' })

  useEffect(() => {
    if (!loggedIn) return
    let ignore = false
    getMyInfo()
      .then((me) => {
        if (!ignore) setState({ status: 'ready', me })
      })
      .catch((e) => {
        if (!ignore) {
          setState({
            status: 'error',
            message: e instanceof ApiError ? e.message : '정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
          })
        }
      })
    return () => {
      ignore = true
    }
  }, [loggedIn])

  // 토큰 재발급이 실패하는 등 로그인이 풀리면 로그인 화면으로
  useEffect(() => {
    const handleExpired = () => navigate('/login', { replace: true })
    window.addEventListener(SESSION_EXPIRED_EVENT, handleExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleExpired)
  }, [navigate])

  const backToLogin = (
    <button
      type="button"
      onClick={() => {
        clearTokens()
        navigate('/login', { replace: true })
      }}
      className="mt-2 h-10 rounded-[10px] bg-primary px-4 text-[14px] font-bold text-surface"
    >
      다른 계정으로 로그인
    </button>
  )

  if (!loggedIn) return <Navigate to="/login" replace />
  if (state.status === 'loading') return <CenterMessage icon="hourglass_empty" message="불러오는 중이에요…" />
  if (state.status === 'error') return <CenterMessage icon="error" message={state.message} action={backToLogin} />
  if (state.me.role !== 'ADMIN') {
    return <CenterMessage icon="lock" message="운영자만 접근할 수 있어요." action={backToLogin} />
  }
  return <AdminLayout nickname={state.me.nickname} />
}
