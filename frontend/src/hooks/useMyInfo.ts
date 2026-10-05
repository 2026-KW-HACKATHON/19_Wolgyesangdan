import { useEffect, useState } from 'react'
import { getMyInfo } from '../api/users'
import { isLoggedIn } from '../lib/authStorage'
import type { MyInfo } from '../types/user'

/** 내 정보 (GET /users/me). 비로그인이면 요청하지 않는다. */
export function useMyInfo() {
  const loggedIn = isLoggedIn()
  const [info, setInfo] = useState<MyInfo | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    if (!loggedIn) return
    let ignore = false
    getMyInfo()
      .then((data) => {
        if (!ignore) setInfo(data)
      })
      .catch(() => {
        if (!ignore) setFailed(true)
      })
    return () => {
      ignore = true
    }
  }, [loggedIn])

  return { info, loggedIn, loading: loggedIn && info === null && !failed, failed }
}
