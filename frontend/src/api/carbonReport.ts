import {
  MOCK_CAMPAIGN_REPORT,
  MOCK_DONG_REPORT,
  type CampaignReport,
  type DongReport,
  type ReportScope,
} from '../data/carbonReport'
import { isLoggedIn } from '../lib/authStorage'

export function fetchCarbonReport(scope: 'dong'): Promise<DongReport>
export function fetchCarbonReport(scope: 'campaign'): Promise<CampaignReport>
export function fetchCarbonReport(scope: ReportScope): Promise<DongReport | CampaignReport>
/**
 * 탄소절감 리포트 조회. 비로그인도 볼 수 있고, 로그인 상태면 me(내 기여분)가 함께 온다.
 * TODO: 백엔드 GET /carbon-report 구현 후 apiFetch(`/carbon-report?scope=${scope}`)로 교체
 */
export async function fetchCarbonReport(scope: ReportScope) {
  const report = scope === 'dong' ? MOCK_DONG_REPORT : MOCK_CAMPAIGN_REPORT
  return isLoggedIn() ? report : { ...report, me: undefined }
}
