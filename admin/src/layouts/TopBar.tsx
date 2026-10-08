import MaterialIcon from '../components/MaterialIcon'
import type { SummaryCampaign } from '../types/summary'

/** YYYY-MM-DD → "9.20" */
function monthDay(isoDate: string) {
  const [, month, day] = isoDate.split('-').map(Number)
  return `${month}.${day}`
}

/** 진행 중(없으면 예정) 캠페인 칩 — 캠페인 전체 기간(물품 등록 시작일부터 수령 마감일까지) */
function CampaignChip({ campaign }: { campaign: SummaryCampaign }) {
  return (
    <span className="flex items-center gap-1.5 rounded-full bg-amber-badge px-3 py-1.5 text-[12px] font-bold text-amber-badge-ink">
      <MaterialIcon name="campaign" size={16} />
      {campaign.name} {campaign.status === 'ACTIVE' ? '진행 중' : '예정'} · {monthDay(campaign.startDate)} ~{' '}
      {monthDay(campaign.endDate)}
    </span>
  )
}

interface TopBarProps {
  title: string
  subtitle: string
  /** 요약(GET /admin/summary)의 현재 캠페인. 없거나 아직 못 불러왔으면 null — 칩을 숨긴다 */
  campaign: SummaryCampaign | null
}

/** 상단 바 64px — 페이지 제목 · 부제 · 캠페인 칩 */
export default function TopBar({ title, subtitle, campaign }: TopBarProps) {
  return (
    <header className="flex h-16 flex-none items-center gap-3 border-b border-border bg-surface px-8">
      <h1 className="text-[20px] font-extrabold text-label">{title}</h1>
      <p className="text-[13px] text-label-alt">{subtitle}</p>
      <div className="ml-auto">{campaign && <CampaignChip campaign={campaign} />}</div>
    </header>
  )
}
