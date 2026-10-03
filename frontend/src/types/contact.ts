export type ContactType = 'openchat' | 'phone'

/** 배정된 상대에게만 공개되는 연락 수단. 하나만 저장한다. */
export interface Contact {
  type: ContactType
  value: string
  verified: boolean
}
