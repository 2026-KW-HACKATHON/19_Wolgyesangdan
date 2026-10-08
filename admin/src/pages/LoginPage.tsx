import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { adminLogin } from '../api/auth'
import { ApiError } from '../api/client'
import { isLoggedIn, saveTokens } from '../lib/authStorage'

/** 관리자 로그인 — 아이디·비밀번호 (POST /auth/admin/login). 계정은 서버 설정값이다 */
export default function LoginPage() {
  const navigate = useNavigate()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (isLoggedIn()) return <Navigate to="/" replace />

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (submitting) return
    setSubmitting(true)
    setError(null)
    try {
      saveTokens(await adminLogin(loginId.trim(), password))
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인하지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass =
    'h-11 w-full rounded-[10px] border border-border bg-surface px-3.5 text-[14px] text-label outline-none focus:border-primary'

  return (
    <div className="flex h-full items-center justify-center bg-admin-sidebar">
      <form onSubmit={handleSubmit} className="w-[360px] rounded-2xl bg-surface p-8">
        <div className="mb-7 flex items-center gap-2">
          <span className="font-hand text-[30px] leading-none font-bold text-admin-sidebar">월계장터</span>
          <span className="rounded-md bg-primary-soft px-1.5 py-0.5 text-[11px] font-bold text-admin-sidebar">관리자</span>
        </div>

        <label htmlFor="login-id" className="mb-1.5 block text-[13px] font-bold text-body">
          아이디
        </label>
        <input
          id="login-id"
          autoComplete="username"
          value={loginId}
          onChange={(e) => setLoginId(e.target.value)}
          className={inputClass}
        />

        <label htmlFor="password" className="mt-4 mb-1.5 block text-[13px] font-bold text-body">
          비밀번호
        </label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className={inputClass}
        />

        {error && (
          <p role="alert" className="mt-3 text-[13px] font-semibold text-terracotta">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={!loginId.trim() || !password || submitting}
          className="mt-6 h-11 w-full rounded-[10px] bg-primary text-[15px] font-bold text-surface disabled:opacity-50"
        >
          {submitting ? '로그인 중…' : '로그인'}
        </button>
      </form>
    </div>
  )
}
