import { CAMPAIGN } from './campaign'

// TODO: 백엔드 연결 후 GET /report?scope=dong|campaign 응답으로 대체
export type ReportScope = 'dong' | 'campaign'

export interface ReportBar {
  label: string
  kg: number
}

export interface ReportCategory {
  category: string
  kg: number
}

export interface CarbonReport {
  /** 누적 기준을 설명하는 배지 문구 */
  periodLabel: string
  reusedCount: number
  co2eTotalKg: number
  trendTitle: string
  trendSubtitle: string
  trend: ReportBar[]
  byCategory: ReportCategory[]
}

const DONG_REPORT: CarbonReport = {
  periodLabel: '2025.03부터 누적',
  reusedCount: 612,
  co2eTotalKg: 15840,
  trendTitle: '월별 탄소 절감 추이',
  trendSubtitle: '최근 6개월 · kg CO₂e (예상치)',
  trend: [
    { label: '4월', kg: 1800 },
    { label: '5월', kg: 2100 },
    { label: '6월', kg: 2500 },
    { label: '7월', kg: 1800 },
    { label: '8월', kg: 2700 },
    { label: '9월', kg: 4300 },
  ],
  byCategory: [
    { category: '가구', kg: 6120 },
    { category: '가전', kg: 4380 },
    { category: '주방', kg: 2260 },
    { category: '생활', kg: 1980 },
    { category: '기타', kg: 1100 },
  ],
}

/** 캠페인 일별 값 (오늘부터 5일, 임시 값) */
const CAMPAIGN_DAILY = [520, 640, 610, 700, 580]

/** 오늘부터 count일 동안의 "M.D" 라벨. 캠페인 기간이 확정되면 서버 값으로 대체한다. */
function upcomingDayLabels(count: number) {
  const today = new Date()
  return Array.from({ length: count }, (_, i) => {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i)
    return `${d.getMonth() + 1}.${d.getDate()}`
  })
}

const CAMPAIGN_DAY_LABELS = upcomingDayLabels(CAMPAIGN_DAILY.length)

const CAMPAIGN_REPORT: CarbonReport = {
  periodLabel: `${CAMPAIGN_DAY_LABELS[0]} ~ ${CAMPAIGN_DAY_LABELS[CAMPAIGN_DAY_LABELS.length - 1]}`,
  reusedCount: CAMPAIGN.reusedCount,
  co2eTotalKg: CAMPAIGN.carbonKg,
  trendTitle: '캠페인 기간 탄소 절감 추이',
  trendSubtitle: '최근 5일 · kg CO₂e (예상치)',
  trend: CAMPAIGN_DAILY.map((kg, i) => ({ label: CAMPAIGN_DAY_LABELS[i], kg })),
  byCategory: [
    { category: '가구', kg: 1100 },
    { category: '가전', kg: 900 },
    { category: '주방', kg: 500 },
    { category: '생활', kg: 520 },
    { category: '기타', kg: 400 },
  ],
}

export const CARBON_REPORTS: Record<ReportScope, CarbonReport> = {
  dong: DONG_REPORT,
  campaign: CAMPAIGN_REPORT,
}

/** 나무 1그루당 연간 흡수량 6kg CO₂e 가정 */
export function treeEquivalent(co2eKg: number) {
  return Math.floor(co2eKg / 6)
}
