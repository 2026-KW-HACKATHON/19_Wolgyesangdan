import type { CampaignStatus } from './campaign'

/** 요약의 현재 캠페인. 기간은 등록·신청·수령 기간 전체, reusedCount는 거래가 끝난 물품 수 */
export interface SummaryCampaign {
  id: number
  name: string
  status: CampaignStatus
  startDate: string
  endDate: string
  reusedCount: number
}

/** GET /admin/summary — 대시보드 카드 · 사이드바 건수 배지 · 상단 바 캠페인 칩에 쓴다 */
export interface AdminSummary {
  pendingVerifications: number
  openInquiries: number
  hiddenItems: number
  /** 진행 중(없으면 다음 예정) 캠페인, 없으면 null */
  currentCampaign: SummaryCampaign | null
}

/**
 * 대시보드 "최근 들어온 서류"에 쓰는 GET /admin/verifications 한 줄의 일부.
 * 서류 화면(#208)의 타입과 겹치지 않게 대시보드에서 쓰는 필드만 여기에 둔다.
 */
export interface RecentVerification {
  id: number
  nickname: string
  verificationType: 'FRESHMAN' | 'LOW_INCOME'
  submittedAt: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'
}
