import type { ReportCampaign } from '../../data/carbonReport'
import { treesFromKg } from '../../lib/carbonTree'
import MaterialIcon from '../icons/MaterialIcon'
import { formatMonthDay, formatNumber } from './format'
import MetricPair from './MetricPair'

const STATUS_LABEL: Record<ReportCampaign['status'], string> = {
  planned: '예정',
  active: '진행 중',
  ended: '종료',
}

interface CampaignSummaryCardProps {
  campaign: ReportCampaign
  reusedCount: number
  co2eTotalKg: number
}

/** 5c ①′ 캠페인 요약 카드 */
export default function CampaignSummaryCard({ campaign, reusedCount, co2eTotalKg }: CampaignSummaryCardProps) {
  return (
    <section className="mx-5 mt-3 rounded-3xl border border-border bg-surface px-[18px] pt-[18px] pb-4">
      <div className="flex items-center gap-2">
        <span
          className={`flex-none rounded-[7px] px-2 py-[3px] text-[11px] font-bold ${
            campaign.status === 'active' ? 'bg-terracotta text-surface' : 'bg-sunken text-ink-2'
          }`}
        >
          {STATUS_LABEL[campaign.status]}
        </span>
        <span className="min-w-0 truncate text-[13px] font-semibold text-label-alt">
          {campaign.title} · {formatMonthDay(campaign.startAt)} ~ {formatMonthDay(campaign.endAt)}
        </span>
      </div>
      <div className="mt-3.5">
        <MetricPair countLabel="거래된 물품" count={reusedCount} co2eKg={co2eTotalKg} />
      </div>
      <div className="mt-3.5 flex items-center gap-2 rounded-xl bg-primary-tint px-3 py-2.5">
        <MaterialIcon name="park" size={18} color="var(--color-accent)" />
        <span className="text-[13px] font-semibold text-[#2B331F]">
          이번 캠페인으로 나무 {formatNumber(treesFromKg(co2eTotalKg))}그루를 키웠어요
        </span>
      </div>
    </section>
  )
}
