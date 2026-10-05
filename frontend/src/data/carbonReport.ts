// TODO: 백엔드 연결 후 GET /carbon-report?scope=dong|campaign 응답으로 대체 (api/carbonReport.ts)
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

function shares(entries: [CategoryGroup, number][]): CategoryShare[] {
  const total = entries.reduce((sum, [, kg]) => sum + kg, 0)
  return entries.map(([category, kg]) => ({ category, kg, ratio: kg / total }))
}

/** 오늘 기준 offset일 뒤의 YYYY-MM-DD (시안 날짜를 오늘에 맞춰 보여주기 위한 임시 값) */
function dayFromToday(offset: number) {
  const now = new Date()
  const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + offset)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}

export const MOCK_DONG_REPORT: DongReport = {
  scope: 'dong',
  since: '2025-03',
  reusedCount: 612,
  co2eTotalKg: 15840,
  byCategory: shares([
    ['가구', 6120],
    ['가전', 4380],
    ['주방', 2260],
    ['생활', 1980],
    ['기타', 1100],
  ]),
  me: { co2eKg: 83 },
}

/** 시안 5c와 같은 모양: 15일짜리 캠페인의 10일째가 오늘 */
const CAMPAIGN_DAILY_COUNTS = [6, 9, 11, 8, 14, 17, 12, 15, 19, 17]

export const MOCK_CAMPAIGN_REPORT: CampaignReport = {
  scope: 'campaign',
  campaign: {
    id: 1,
    title: '2026 자원순환 캠페인',
    startAt: dayFromToday(-(CAMPAIGN_DAILY_COUNTS.length - 1)),
    endAt: dayFromToday(5),
    status: 'active',
  },
  reusedCount: 128,
  co2eTotalKg: 3420,
  daily: CAMPAIGN_DAILY_COUNTS.map((count, i) => ({
    date: dayFromToday(i - (CAMPAIGN_DAILY_COUNTS.length - 1)),
    count,
  })),
  byCategory: shares([
    ['가구', 1334],
    ['가전', 958],
    ['주방', 479],
    ['생활', 410],
    ['기타', 239],
  ]),
  me: { co2eKg: 41 },
}
