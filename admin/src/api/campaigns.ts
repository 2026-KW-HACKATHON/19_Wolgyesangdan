import type { ActiveCampaign } from '../types/campaign'
import { apiFetch } from './client'

/** 진행 중(없으면 다음 예정) 캠페인 (GET /campaigns/active). 없으면 null — 상단 바 캠페인 칩에 쓴다 */
export function getActiveCampaign() {
  return apiFetch<ActiveCampaign | null>('/campaigns/active')
}
