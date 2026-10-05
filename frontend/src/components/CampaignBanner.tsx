import { formatCampaignDday, formatCampaignPeriod } from '../lib/campaign'
import type { ActiveCampaign } from '../types/campaign'

type CampaignBannerProps = {
  /** GET /campaigns/active 응답. null이면 "캠페인 없음" 안내를 보여준다 (UI-03) */
  campaign: ActiveCampaign | null
  onDetail?: () => void
}

export default function CampaignBanner({ campaign, onDetail }: CampaignBannerProps) {
  if (!campaign) {
    return (
      <div className="mx-5 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-4.5 py-4">
        <h2 className="text-[17px] font-bold text-[var(--color-label)]">예정된 캠페인이 없어요</h2>
        <p className="mt-1 text-[13px] font-medium text-[#6E7263]">지금은 직거래만 가능해요</p>
      </div>
    )
  }

  const planned = campaign.status === 'PLANNED'
  return (
    <div className="mx-5 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-4.5 py-4">
      <div className="mb-2 flex items-center gap-2">
        <span
          className={`rounded-md px-2 py-0.5 text-[11px] font-bold text-[var(--color-surface)] ${
            planned ? 'bg-[var(--color-label-alt)]' : 'bg-[#B9603A]'
          }`}
        >
          {planned ? '예정' : '진행 중'}
        </span>
        <span className={`text-[13px] font-bold ${planned ? 'text-[var(--color-label-alt)]' : 'text-[#B9603A]'}`}>
          {formatCampaignDday(campaign)}
        </span>
      </div>
      <h2 className="text-xl font-bold text-[var(--color-label)]">{campaign.name}</h2>
      <p className="mt-1 text-[13px] font-medium text-[#6E7263]">
        {formatCampaignPeriod(campaign)} · {campaign.locationName}
      </p>
      <div className="mt-3.5 flex items-center gap-4">
        <div>
          <div className="text-[19px] font-bold text-[var(--color-label)]">
            {campaign.reusedCount.toLocaleString()}개
          </div>
          <div className="mt-0.5 text-[11px] font-medium text-[var(--color-label-alt)]">
            이번 캠페인 재사용
          </div>
        </div>
        <div className="h-8 w-px bg-[var(--color-border)]" />
        <div>
          <div className="text-[19px] font-bold text-[var(--color-accent)]">
            {campaign.carbonReductionKg.toLocaleString()}kg
          </div>
          <div className="mt-0.5 text-[11px] font-medium text-[var(--color-label-alt)]">
            누적 CO₂e 절감 (예상치)
          </div>
        </div>
      </div>
      <button
        type="button"
        onClick={onDetail}
        className="mt-3.5 flex w-full items-center justify-center gap-0.5 rounded-xl bg-[var(--color-primary)] py-2.5 text-[13px] font-bold text-[var(--color-surface)]"
      >
        자세히 보기
        <span className="ms text-base">chevron_right</span>
      </button>
    </div>
  )
}
