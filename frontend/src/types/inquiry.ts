export type InquiryCategory = 'TRADE' | 'VERIFICATION' | 'HUB' | 'ETC'

export type InquiryStatus = 'OPEN' | 'ANSWERED'

/** 내 문의 (POST /inquiries, GET /users/me/inquiries). 답변 전이면 answer·answeredAt은 null */
export interface Inquiry {
  id: number
  category: InquiryCategory
  title: string
  content: string
  status: InquiryStatus
  answer: string | null
  answeredAt: string | null
  createdAt: string
}
