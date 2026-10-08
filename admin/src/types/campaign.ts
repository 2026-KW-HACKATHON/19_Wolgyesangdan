export type CampaignStatus = 'PLANNED' | 'ACTIVE' | 'ENDED'

/** GET /campaigns/active 응답 중 관리자 웹에서 쓰는 부분. 날짜는 YYYY-MM-DD */
export interface ActiveCampaign {
  id: number
  name: string
  status: CampaignStatus
  registrationStartDate: string
  pickupEndDate: string
}

/** 관리자 캠페인 폼에 채우는 캠페인 전체 정보 (AdminCampaignResponse). status가 ENDED가 아니면 "운영 중"이 켜진 캠페인 */
export interface AdminCampaign {
  id: number
  name: string
  description: string | null
  status: CampaignStatus
  registrationStartDate: string
  registrationEndDate: string
  applicationStartDate: string
  applicationEndDate: string
  pickupStartDate: string
  pickupEndDate: string
  locationName: string
  locationAddress: string
  hubHours: string | null
}

/** 지난 캠페인 한 줄. 기간은 등록·신청·수령 기간 전체 */
export interface PastCampaign {
  id: number
  name: string
  startDate: string
  endDate: string
  reusedCount: number
  carbonReductionKg: number
}

/** GET /admin/campaigns — current는 예정이거나 진행 중인 캠페인(없으면 null), past는 최근에 끝난 순 */
export interface AdminCampaignList {
  current: AdminCampaign | null
  past: PastCampaign[]
}

/** POST /admin/campaigns 본문. description·hubHours만 선택 */
export interface AdminCampaignCreateRequest {
  name: string
  description?: string
  registrationStartDate: string
  registrationEndDate: string
  applicationStartDate: string
  applicationEndDate: string
  pickupStartDate: string
  pickupEndDate: string
  locationName: string
  locationAddress: string
  hubHours?: string
}

/** PATCH /admin/campaigns/{id} 본문 — 바꿀 필드만. description·hubHours는 빈 문자열이면 지워진다. status는 운영 중 토글 */
export type AdminCampaignUpdateRequest = Partial<AdminCampaignCreateRequest> & { status?: 'ACTIVE' | 'ENDED' }
