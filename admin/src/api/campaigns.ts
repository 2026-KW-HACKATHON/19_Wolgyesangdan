import type {
  ActiveCampaign,
  AdminCampaign,
  AdminCampaignCreateRequest,
  AdminCampaignList,
  AdminCampaignUpdateRequest,
} from '../types/campaign'
import { apiFetch } from './client'

/** 진행 중(없으면 다음 예정) 캠페인 (GET /campaigns/active). 없으면 null — 상단 바 캠페인 칩에 쓴다 */
export function getActiveCampaign() {
  return apiFetch<ActiveCampaign | null>('/campaigns/active')
}

/** 현재(예정·진행 중) 캠페인과 지난 캠페인 목록 (GET /admin/campaigns) */
export function getAdminCampaigns() {
  return apiFetch<AdminCampaignList>('/admin/campaigns')
}

/** 새 캠페인 (POST /admin/campaigns). 예정·진행 중 캠페인이 이미 있으면 409 CAMPAIGN_ALREADY_RUNNING */
export function createAdminCampaign(request: AdminCampaignCreateRequest) {
  return apiFetch<AdminCampaign>('/admin/campaigns', { method: 'POST', body: JSON.stringify(request) })
}

/** 캠페인 수정 (PATCH /admin/campaigns/{id}) — 바꿀 필드만 보낸다 */
export function updateAdminCampaign(campaignId: number, request: AdminCampaignUpdateRequest) {
  return apiFetch<AdminCampaign>(`/admin/campaigns/${campaignId}`, { method: 'PATCH', body: JSON.stringify(request) })
}
