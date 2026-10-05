import { createContext, useContext } from 'react'
import type { Contact } from '../types/contact'

export interface ContactContextValue {
  /** 서버에 저장된 내 연락 수단. 비로그인이거나 설정한 적 없으면 null */
  contact: Contact | null
  /** 로그인 상태에서 아직 서버 값을 받는 중 */
  loading: boolean
  /** 서버에 저장한다. 실패하면 ApiError를 던진다 */
  saveContact: (contact: Contact) => Promise<void>
}

export const ContactContext = createContext<ContactContextValue | null>(null)

export function useContact() {
  const value = useContext(ContactContext)
  if (!value) throw new Error('useContact는 ContactProvider 안에서만 쓸 수 있습니다.')
  return value
}
