import type { InquiryCategory, InquiryStatus } from '../types/inquiry'

/** 문의 카테고리 — 작성 화면의 칩 순서이기도 하다 */
export const INQUIRY_CATEGORIES: InquiryCategory[] = ['TRADE', 'VERIFICATION', 'HUB', 'ETC']

export const INQUIRY_CATEGORY_LABEL: Record<InquiryCategory, string> = {
  TRADE: '거래',
  VERIFICATION: '인증',
  HUB: '거점',
  ETC: '기타',
}

export const INQUIRY_STATUS_LABEL: Record<InquiryStatus, string> = {
  OPEN: '답변 대기',
  ANSWERED: '답변 완료',
}

/** 상태 배지 색 — 인증 현황(MyVerificationPage)의 검토 중·완료와 같은 색 */
export const INQUIRY_STATUS_BADGE: Record<InquiryStatus, string> = {
  OPEN: 'bg-amber-badge text-amber-badge-ink',
  ANSWERED: 'bg-primary-tint text-primary-tint-ink',
}

/** "2026.10.8" */
export function formatInquiryDate(isoDateTime: string) {
  const date = new Date(isoDateTime)
  return `${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()}`
}
