export type InquiryCategory = 'TRADE' | 'VERIFICATION' | 'HUB' | 'ETC'

export type InquiryStatus = 'OPEN' | 'ANSWERED'

/** GET /admin/inquiries 의 한 줄 (AdminInquirySummaryResponse). nickname은 작성자 */
export interface AdminInquirySummary {
  id: number
  category: InquiryCategory
  title: string
  nickname: string
  createdAt: string
  status: InquiryStatus
}

/** GET /admin/inquiries/{id} (AdminInquiryDetailResponse) — 목록 항목 + 본문·답변 */
export interface AdminInquiryDetail extends AdminInquirySummary {
  content: string
  answer: string | null
  answeredAt: string | null
}
