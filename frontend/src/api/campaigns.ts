import type { ActiveCampaign } from '../types/campaign'
import { apiFetch } from './client'

/** 진행 중(없으면 다음 예정) 캠페인 (GET /campaigns/active, 비회원 허용). 캠페인이 없으면 null */
export function getActiveCampaign() {
  return apiFetch<ActiveCampaign | null>('/campaigns/active')
}
