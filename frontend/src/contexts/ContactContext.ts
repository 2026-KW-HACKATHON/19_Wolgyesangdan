import { createContext, useContext } from 'react'
import type { Contact } from '../types/contact'

export interface ContactContextValue {
  contact: Contact | null
  saveContact: (contact: Contact) => void
}

export const ContactContext = createContext<ContactContextValue | null>(null)

export function useContact() {
  const value = useContext(ContactContext)
  if (!value) throw new Error('useContact는 ContactProvider 안에서만 쓸 수 있습니다.')
  return value
}
