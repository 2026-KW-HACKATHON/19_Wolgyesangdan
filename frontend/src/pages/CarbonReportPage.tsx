import { useState } from 'react'
import MaterialIcon from '../components/icons/MaterialIcon'
import { CAMPAIGN } from '../data/campaign'
import { CARBON_REPORTS, treeEquivalent, type ReportScope } from '../data/carbonReport'

const SCOPE_TABS: { value: ReportScope; label: string }[] = [
  { value: 'dong', label: '월계1동 전체' },
  { value: 'campaign', label: '이번 캠페인' },
]

const MAX_BAR_PX = 64

function formatNumber(n: number) {
  return n.toLocaleString('ko-KR')
}

/** 탄소절감 리포트 (탭 4, /carbon-report). 로그인 없이 볼 수 있다. */
export default function CarbonReportPage() {
  const [scope, setScope] = useState<ReportScope>('dong')
  const report = CARBON_REPORTS[scope]
  const campaignActive = CAMPAIGN.active

  const trendMax = Math.max(...report.trend.map((b) => b.kg), 1)
  const categories = [...report.byCategory].sort((a, b) => b.kg - a.kg)
  const trees = treeEquivalent(report.co2eTotalKg)

  return (
    <div className="flex min-h-0 flex-1 flex-col pb-4">
      <div className="px-5 pt-5">
        <h1 className="font-hand text-[28px] leading-[1.2] font-bold text-label">탄소절감 리포트</h1>
        <p className="mt-1 text-[13px] font-medium text-label-alt">월계1동의 자원순환 성과를 확인해보세요</p>
      </div>

      {/* 세그먼트 탭 */}
      <div className="mx-5 mt-3.5 flex gap-1 rounded-[14px] bg-sunken p-1" role="tablist">
        {SCOPE_TABS.map((tab) => {
          const selected = scope === tab.value
          const disabled = tab.value === 'campaign' && !campaignActive
          return (
            <button
              key={tab.value}
              type="button"
              role="tab"
              aria-selected={selected}
              disabled={disabled}
              onClick={() => setScope(tab.value)}
              className={`flex-1 rounded-[11px] py-2.5 text-[14px] disabled:cursor-default disabled:text-ink-disabled ${
                selected ? 'bg-surface font-bold text-label' : 'font-semibold text-label-alt'
              }`}
            >
              {tab.label}
            </button>
          )
        })}
      </div>
      {!campaignActive && (
        <p className="mx-5 mt-1.5 text-[12px] font-medium text-label-alt">진행 중인 캠페인이 없어요</p>
      )}

      {/* 누적 카드 */}
      <div className="mx-5 mt-3 rounded-[18px] border border-border bg-surface px-[18px] py-4">
        <span className="rounded-[7px] bg-sunken px-2 py-[3px] text-[11px] font-semibold text-ink-2">
          {report.periodLabel}
        </span>
        <div className="mt-3 flex gap-4">
          <div className="flex flex-1 flex-col gap-1">
            <div className="text-[12px] font-medium text-label-alt">재사용된 물품</div>
            <div className="flex items-baseline gap-[3px]">
              <span className="text-[26px] font-extrabold tracking-[-0.02em] text-label">
                {formatNumber(report.reusedCount)}
              </span>
              <span className="text-[14px] font-bold text-label">개</span>
            </div>
          </div>
          <div className="w-px bg-border" />
          <div className="flex flex-1 flex-col gap-1">
            <div className="text-[12px] font-medium text-label-alt">누적 CO₂e 절감</div>
            <div className="flex items-baseline gap-[3px]">
              <span className="text-[26px] font-extrabold tracking-[-0.02em] text-accent">
                {formatNumber(report.co2eTotalKg)}
              </span>
              <span className="text-[14px] font-bold text-accent">kg</span>
            </div>
          </div>
        </div>
        <div className="mt-2.5 text-[11px] font-medium text-label-alt">
          모든 절감량은 재사용 시 예상되는 값을 더한 예상치예요
        </div>
      </div>

      {/* 나무 환산 */}
      <div className="mx-5 mt-3 flex items-center gap-3 rounded-2xl bg-primary-tint px-4 py-3.5">
        <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-surface text-accent">
          <MaterialIcon name="park" size={20} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-[14px] font-bold text-primary-tint-ink">
            소나무 약 {formatNumber(trees)}그루가 1년간 흡수하는 양과 비슷해요
          </div>
          <div className="mt-[3px] text-[11px] font-medium text-primary-tint-ink opacity-80">
            나무 1그루당 연간 흡수량 6kg CO₂e 가정 · 추정치
          </div>
        </div>
      </div>

      {/* 추이 차트 */}
      <div className="px-5 pt-5.5 pb-2.5">
        <h2 className="font-hand text-[22px] leading-[1.3] font-bold text-label">{report.trendTitle}</h2>
        <p className="mt-0.5 text-[13px] font-medium text-label-alt">{report.trendSubtitle}</p>
      </div>
      <div className="flex h-24 items-end gap-2.5 px-6">
        {report.trend.map((bar, i) => {
          const isLatest = i === report.trend.length - 1
          const height = Math.max((bar.kg / trendMax) * MAX_BAR_PX, 4)
          return (
            <div key={bar.label} className="flex flex-1 flex-col items-center gap-1.5">
              <div
                title={`${bar.label} · ${formatNumber(bar.kg)}kg`}
                className={`w-full max-w-7 rounded-[7px] ${isLatest ? 'bg-primary' : 'bg-primary-soft'}`}
                style={{ height }}
              />
              <span
                className={`text-[11px] whitespace-nowrap ${
                  isLatest ? 'font-bold text-accent' : 'font-medium text-label-alt'
                }`}
              >
                {bar.label}
              </span>
            </div>
          )
        })}
      </div>

      <div className="mt-5 h-2 bg-sunken" />

      {/* 카테고리별 */}
      <div className="px-5 pt-5 pb-2.5">
        <h2 className="font-hand text-[22px] leading-[1.3] font-bold text-label">카테고리별 절감량</h2>
        <p className="mt-0.5 text-[13px] font-medium text-label-alt">누적 기준 (예상치)</p>
      </div>
      <div className="flex flex-col gap-3 px-5">
        {categories.map((item) => {
          const percent = Math.round((item.kg / report.co2eTotalKg) * 100)
          return (
            <div key={item.category} className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[13px] font-bold text-body">{item.category}</span>
                <span className="text-[13px] font-medium text-label-alt">
                  {formatNumber(item.kg)}kg · {percent}%
                </span>
              </div>
              <div className="h-2 overflow-hidden rounded-sm bg-sunken">
                <div className="h-full rounded-sm bg-primary" style={{ width: `${percent}%` }} />
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
