import { useEffect, useState } from 'react'
import { getActiveCampaign } from '../api/campaigns'
import MaterialIcon from '../components/MaterialIcon'
import type { ActiveCampaign } from '../types/campaign'

/** YYYY-MM-DD → "9.20" */
function monthDay(isoDate: string) {
  const [, month, day] = isoDate.split('-').map(Number)
  return `${month}.${day}`
}

/** 진행 중(없으면 예정) 캠페인 칩 — 물품 등록 시작일부터 수령 마감일까지 */
function CampaignChip() {
  const [campaign, setCampaign] = useState<ActiveCampaign | null>(null)

  useEffect(() => {
    let ignore = false
    getActiveCampaign()
      .then((result) => {
        if (!ignore) setCampaign(result)
      })
      .catch(() => {
        // 칩은 참고용이라 못 불러오면 숨긴다
      })
    return () => {
      ignore = true
    }
  }, [])

  if (!campaign) return null
  return (
    <span className="flex items-center gap-1.5 rounded-full bg-amber-badge px-3 py-1.5 text-[12px] font-bold text-amber-badge-ink">
      <MaterialIcon name="campaign" size={16} />
      {campaign.name} {campaign.status === 'ACTIVE' ? '진행 중' : '예정'} · {monthDay(campaign.registrationStartDate)} ~{' '}
      {monthDay(campaign.pickupEndDate)}
    </span>
  )
}

/** 상단 바 64px — 페이지 제목 · 부제 · 캠페인 칩 */
export default function TopBar({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <header className="flex h-16 flex-none items-center gap-3 border-b border-border bg-surface px-8">
      <h1 className="text-[20px] font-extrabold text-label">{title}</h1>
      <p className="text-[13px] text-label-alt">{subtitle}</p>
      <div className="ml-auto">
        <CampaignChip />
      </div>
    </header>
  )
}
