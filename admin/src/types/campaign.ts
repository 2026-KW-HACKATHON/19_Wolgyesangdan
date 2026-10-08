export type CampaignStatus = 'PLANNED' | 'ACTIVE' | 'ENDED'

/** GET /campaigns/active 응답 중 관리자 웹에서 쓰는 부분. 날짜는 YYYY-MM-DD */
export interface ActiveCampaign {
  id: number
  name: string
  status: CampaignStatus
  registrationStartDate: string
  pickupEndDate: string
}
