import type { CarbonReport, ReportScope } from '../data/carbonReport'
import { apiFetch } from './client'

/** 백엔드 GET /users/me/impact 응답 */
export interface MyImpact {
  givenCount: number
  receivedCount: number
  carbonReductionKg: number
}

/**
 * 탄소절감 리포트. 비회원도 볼 수 있고, 로그인 상태면 myCarbonReductionKg가 함께 온다.
 * scope=CAMPAIGN인데 진행 중·예정 캠페인이 없으면 에러가 아니라 campaign: null (전부 0)로 온다.
 */
export function fetchCarbonReport(scope: ReportScope) {
  return apiFetch<CarbonReport>(`/carbon-report?scope=${scope}`)
}

/** 나의 자원순환 기록 (로그인 필요) */
export function fetchMyImpact() {
  return apiFetch<MyImpact>('/users/me/impact')
}
