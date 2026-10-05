import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getMyVerifications } from '../api/verification'
import { isLoggedIn } from '../lib/authStorage'
import type { MyVerification } from '../types/verification'

/** 내 인증 현황 (GET /verifications/me). 비로그인이면 요청하지 않고 빈 목록으로 둔다. */
export function useMyVerifications() {
  const loggedIn = isLoggedIn()
  const [verifications, setVerifications] = useState<MyVerification[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!loggedIn) return
    let ignore = false
    getMyVerifications()
      .then((data) => {
        if (!ignore) setVerifications(data)
      })
      .catch((e: unknown) => {
        if (!ignore) {
          setError(e instanceof ApiError ? e.message : '인증 상태를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
        }
      })
    return () => {
      ignore = true
    }
  }, [loggedIn])

  return {
    verifications: verifications ?? [],
    loading: loggedIn && verifications === null && error === null,
    error,
  }
}
