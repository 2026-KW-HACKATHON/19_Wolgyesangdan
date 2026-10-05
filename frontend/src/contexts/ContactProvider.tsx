import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { getMyContact, updateMyContact } from '../api/users'
import { getAccessToken } from '../lib/authStorage'
import type { Contact } from '../types/contact'
import { ContactContext } from './ContactContext'

/**
 * 연락 수단 상태. 로그인 상태면 서버(GET /users/me/contact)에서 받아 오고,
 * saveContact는 서버(PUT /users/me/contact)에 저장한 뒤 상태를 바꾼다.
 */
export default function ContactProvider({ children }: { children: ReactNode }) {
  // 로그인·로그아웃은 화면 이동과 함께 일어나므로, 이동할 때마다 다시 그려서 토큰을 새로 읽는다
  useLocation()
  const token = getAccessToken()

  // 어떤 로그인(토큰)으로 받은 값인지 함께 둬서, 다른 계정으로 바뀌면 이전 값을 쓰지 않는다
  const [loaded, setLoaded] = useState<{ token: string; contact: Contact | null } | null>(null)

  useEffect(() => {
    if (!token) return
    let ignore = false
    getMyContact()
      .then((contact) => {
        if (!ignore) setLoaded({ token, contact })
      })
      .catch(() => {
        // 못 받아 오면 설정하지 않은 것으로 둔다 (설정 화면에서 다시 저장할 수 있다)
        if (!ignore) setLoaded({ token, contact: null })
      })
    return () => {
      ignore = true
    }
  }, [token])

  const current = token !== null && loaded?.token === token ? loaded : null
  const contact = current?.contact ?? null
  const loading = token !== null && current === null

  const saveContact = useCallback(async (next: Contact) => {
    const saved = await updateMyContact(next)
    // 저장하는 동안 토큰이 재발급됐을 수 있어서 지금 토큰으로 기록한다
    const latestToken = getAccessToken()
    if (latestToken) setLoaded({ token: latestToken, contact: saved ?? next })
  }, [])

  const value = useMemo(() => ({ contact, loading, saveContact }), [contact, loading, saveContact])

  return <ContactContext.Provider value={value}>{children}</ContactContext.Provider>
}
