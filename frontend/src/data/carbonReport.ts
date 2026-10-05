// 탄소절감 리포트 화면용 타입. 백엔드 응답은 api/carbonReport.ts에서 이 형태로 바꿔 쓴다.
export type ReportScope = 'dong' | 'campaign'

export type CategoryGroup = '가구' | '가전' | '주방' | '생활' | '기타'

export interface CategoryShare {
  category: CategoryGroup
  kg: number
  /** 전체 절감량 대비 비율 0~1 */
  ratio: number
}

/** 로그인한 사용자의 기여분. 비로그인이면 응답에 없다. */
export interface MyContribution {
  co2eKg: number
}

export interface DongReport {
  scope: 'dong'
  /** 누적 집계 시작 월 (YYYY-MM) */
  since: string
  reusedCount: number
  co2eTotalKg: number
  byCategory: CategoryShare[]
  me?: MyContribution
}

export interface ReportCampaign {
  id: number
  title: string
  /** YYYY-MM-DD */
  startAt: string
  endAt: string
  status: 'planned' | 'active' | 'ended'
}

export interface DailyTrade {
  /** YYYY-MM-DD */
  date: string
  count: number
}

export interface CampaignReport {
  scope: 'campaign'
  campaign: ReportCampaign
  reusedCount: number
  co2eTotalKg: number
  /** 시작일 ~ 오늘. 남은 날짜 칸은 화면에서 startAt ~ endAt으로 채운다. */
  daily: DailyTrade[]
  byCategory: CategoryShare[]
  me?: MyContribution
}

export type CarbonReport = DongReport | CampaignReport
