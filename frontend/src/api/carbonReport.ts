import type { CampaignReport, CategoryGroup, CategoryShare, DongReport, ReportScope } from '../data/carbonReport'
import { apiFetch } from './client'

/** 백엔드 GET /carbon-report 응답 (API 명세서 · 탄소절감 리포트 조회) */
interface CarbonReportResponse {
  scope: 'ALL' | 'CAMPAIGN'
  reusedCount: number
  carbonReductionKg: number
  /** 첫 거래 완료 월 YYYY-MM, 없으면 null */
  since: string | null
  campaign: {
    id: number
    name: string
    status: 'PLANNED' | 'ACTIVE' | 'ENDED'
    startDate: string
    endDate: string
  } | null
  monthlyTrend: { month: string; carbonReductionKg: number }[]
  dailyTrades: { date: string; count: number }[]
  categoryBreakdown: { categoryGroup: CategoryGroup; carbonReductionKg: number; ratio: number }[]
  /** 로그인 시 같은 범위의 내 기여분, 비로그인이면 null */
  me: { carbonReductionKg: number } | null
}

/** 백엔드 GET /users/me/impact 응답 */
export interface MyImpact {
  givenCount: number
  receivedCount: number
  carbonReductionKg: number
}

const SCOPE_PARAM: Record<ReportScope, CarbonReportResponse['scope']> = {
  dong: 'ALL',
  campaign: 'CAMPAIGN',
}

function toShares(res: CarbonReportResponse): CategoryShare[] {
  return res.categoryBreakdown.map((c) => ({ category: c.categoryGroup, kg: c.carbonReductionKg, ratio: c.ratio }))
}

/** 아직 거래가 없어 시작 월이 없으면 이번 달부터로 보여준다 */
function thisMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

/** 월계1동 전체 누적 리포트. 비로그인도 볼 수 있고, 로그인 상태면 me(내 기여분)가 함께 온다. */
export async function fetchDongReport(): Promise<DongReport> {
  const res = await apiFetch<CarbonReportResponse>(`/carbon-report?scope=${SCOPE_PARAM.dong}`)
  return {
    scope: 'dong',
    since: res.since ?? thisMonth(),
    reusedCount: res.reusedCount,
    co2eTotalKg: res.carbonReductionKg,
    byCategory: toShares(res),
    me: res.me ? { co2eKg: res.me.carbonReductionKg } : undefined,
  }
}

/** 이번 캠페인 리포트. 진행 중·예정 캠페인이 없으면 null */
export async function fetchCampaignReport(): Promise<CampaignReport | null> {
  const res = await apiFetch<CarbonReportResponse>(`/carbon-report?scope=${SCOPE_PARAM.campaign}`)
  if (!res.campaign) return null
  return {
    scope: 'campaign',
    campaign: {
      id: res.campaign.id,
      title: res.campaign.name,
      startAt: res.campaign.startDate,
      endAt: res.campaign.endDate,
      status: res.campaign.status.toLowerCase() as CampaignReport['campaign']['status'],
    },
    reusedCount: res.reusedCount,
    co2eTotalKg: res.carbonReductionKg,
    daily: res.dailyTrades,
    byCategory: toShares(res),
    me: res.me ? { co2eKg: res.me.carbonReductionKg } : undefined,
  }
}

/** 나의 자원순환 기록 (로그인 필요) */
export function fetchMyImpact() {
  return apiFetch<MyImpact>('/users/me/impact')
}
