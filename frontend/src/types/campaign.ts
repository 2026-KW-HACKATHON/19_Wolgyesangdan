// 백엔드 /campaigns API 타입

/** 진행 여부는 서버가 날짜로 판단한다 — 활성 캠페인 조회에는 PLANNED·ACTIVE만 내려온다 */
export type CampaignStatus = 'PLANNED' | 'ACTIVE' | 'ENDED'

/** GET /campaigns/active — 진행 중(없으면 다음 예정) 캠페인. 날짜는 YYYY-MM-DD */
export interface ActiveCampaign {
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
  /** 이 캠페인에서 거래 완료된 물품 수 */
  reusedCount: number
  /** 위 물품들의 예상 탄소 절감량 합계 (kg CO₂e) */
  carbonReductionKg: number
}
