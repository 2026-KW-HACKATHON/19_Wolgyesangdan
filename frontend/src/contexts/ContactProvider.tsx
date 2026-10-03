import { useMemo, useState, type ReactNode } from 'react'
import type { Contact } from '../types/contact'
import { ContactContext } from './ContactContext'

/**
 * 연락 수단 상태.
 * TODO: GET /me/contact 로 초기값을 받고, saveContact 에서 PUT /me/contact 를 호출한다.
 * 지금은 메모리에만 있어서 새로고침하면 사라진다.
 */
export default function ContactProvider({ children }: { children: ReactNode }) {
  const [contact, setContact] = useState<Contact | null>(null)

  const value = useMemo(
    () => ({ contact, saveContact: setContact }),
    [contact],
  )

  return <ContactContext.Provider value={value}>{children}</ContactContext.Provider>
}
