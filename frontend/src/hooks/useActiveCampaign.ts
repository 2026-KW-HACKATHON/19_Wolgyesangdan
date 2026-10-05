import { useEffect, useState } from 'react'
import { getActiveCampaign } from '../api/campaigns'
import type { ActiveCampaign } from '../types/campaign'

/**
 * 진행 중(없으면 다음 예정) 캠페인 (GET /campaigns/active).
 * campaign — undefined: 불러오는 중 또는 실패, null: 캠페인 없음
 */
export function useActiveCampaign() {
  const [campaign, setCampaign] = useState<ActiveCampaign | null | undefined>(undefined)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let ignore = false
    getActiveCampaign()
      .then((data) => {
        if (!ignore) setCampaign(data)
      })
      .catch(() => {
        if (!ignore) setFailed(true)
      })
    return () => {
      ignore = true
    }
  }, [])

  return {
    campaign,
    loading: campaign === undefined && !failed,
    failed,
    /** 지금 진행 중인 캠페인이 있는지 — 거점 거래 필터·"이번 캠페인" 탭 노출 기준 */
    active: campaign?.status === 'ACTIVE',
  }
}
