import type { Contact } from '../types/contact'
import { apiFetch } from './client'

/** GET·PUT /users/me/contact 응답. 아직 설정하지 않았으면 contactType이 null */
interface ContactResponse {
  contactType: 'PHONE' | 'OPENCHAT' | null
  phone: string | null
  openchatLink: string | null
}

// 서버에는 두 값이 다 남아 있을 수 있다 — contactType으로 고른 쪽만 현재 연락 수단이다
function toContact(response: ContactResponse): Contact | null {
  if (response.contactType === 'OPENCHAT' && response.openchatLink) {
    return { type: 'openchat', value: response.openchatLink, verified: true }
  }
  if (response.contactType === 'PHONE' && response.phone) {
    return { type: 'phone', value: response.phone, verified: true }
  }
  return null
}

/** 내 연락 수단 (GET /users/me/contact, 로그인 필요). 설정한 적 없으면 null */
export async function getMyContact() {
  return toContact(await apiFetch<ContactResponse>('/users/me/contact'))
}

/** 연락 수단 설정 (PUT /users/me/contact). 형식이 틀리면 400 INVALID_INPUT */
export async function updateMyContact(contact: Contact) {
  const body =
    contact.type === 'openchat'
      ? { contactType: 'OPENCHAT', openchatLink: contact.value }
      : { contactType: 'PHONE', phone: contact.value }
  return toContact(await apiFetch<ContactResponse>('/users/me/contact', { method: 'PUT', body: JSON.stringify(body) }))
}
