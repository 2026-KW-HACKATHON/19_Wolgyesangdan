// 탄소절감 리포트 타입 — 백엔드 GET /carbon-report 응답 그대로 (API 명세서 · 탄소절감 리포트 조회, 2026-10-05)
export type ReportScope = 'ALL' | 'CAMPAIGN'

export type CategoryGroup = '가구' | '가전' | '주방' | '생활' | '기타'

export interface CategoryCarbon {
  categoryGroup: CategoryGroup
  carbonReductionKg: number
  /** 전체 절감량 대비 비율 0~1 */
  ratio: number
}

export interface ReportCampaign {
  id: number
  name: string
  status: 'PLANNED' | 'ACTIVE' | 'ENDED'
  /** YYYY-MM-DD — 등록·신청·수령 기간 중 가장 이른 시작일 ~ 가장 늦은 종료일 */
  startDate: string
  endDate: string
}

export interface DailyTrade {
  /** YYYY-MM-DD */
  date: string
  count: number
}

export interface CarbonReport {
  scope: ReportScope
  /** 거래 완료된 물품 수 */
  reusedCount: number
  /** 위 물품들의 예상 탄소 절감량 합계 (kg CO₂e) */
  carbonReductionKg: number
  /** 대분류 5개 고정 순서 */
  categoryBreakdown: CategoryCarbon[]
  /** 로그인 시 내가 등록해서(나눔) 거래 완료된 물품의 탄소 합계. 비회원이면 null */
  myCarbonReductionKg: number | null
  /** ALL일 때만 첫 거래 완료 월 YYYY-MM. 거래가 없거나 CAMPAIGN이면 null */
  since: string | null
  /** CAMPAIGN일 때만 대상 캠페인. 캠페인이 없거나 ALL이면 null */
  campaign: ReportCampaign | null
  /** CAMPAIGN일 때만 캠페인 시작일 ~ 오늘 날짜별 거래 수. 남은 날짜 칸은 화면에서 채운다 */
  dailyTrend: DailyTrade[]
}
