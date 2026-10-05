import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchCarbonReport } from '../api/carbonReport'
import CampaignSummaryCard from '../components/carbon/CampaignSummaryCard'
import CategoryTreeList from '../components/carbon/CategoryTreeList'
import DailyTradeChart from '../components/carbon/DailyTradeChart'
import MyTreeCard from '../components/carbon/MyTreeCard'
import TreeLevelCard from '../components/carbon/TreeLevelCard'
import type { CarbonReport, ReportScope } from '../data/carbonReport'

const SCOPE_TABS: { value: ReportScope; label: string }[] = [
  { value: 'ALL', label: '월계1동 전체' },
  { value: 'CAMPAIGN', label: '이번 캠페인' },
]

/** 탄소절감 리포트 (탭 4, /carbon-report?scope=ALL|CAMPAIGN). 로그인 없이 볼 수 있다. */
export default function CarbonReportPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [allReport, setAllReport] = useState<CarbonReport | null>(null)
  /** undefined: 불러오는 중, null: 진행 중·예정 캠페인이 없거나 불러오지 못함 */
  const [campaignReport, setCampaignReport] = useState<CarbonReport | null | undefined>(undefined)
  const [campaignFailed, setCampaignFailed] = useState(false)
  const [allFailed, setAllFailed] = useState(false)
  const [attempt, setAttempt] = useState(0)

  // 두 탭을 처음에 함께 불러온다 — 탭 전환이 바로 되고, 캠페인이 없으면 탭을 미리 비활성화할 수 있다.
  // 따로 받아서 캠페인 쪽만 실패해도 월계1동 전체는 보여준다.
  useEffect(() => {
    let cancelled = false
    Promise.allSettled([fetchCarbonReport('ALL'), fetchCarbonReport('CAMPAIGN')]).then(([all, campaign]) => {
      if (cancelled) return
      if (all.status === 'fulfilled') setAllReport(all.value)
      else setAllFailed(true)
      if (campaign.status === 'fulfilled') {
        setCampaignReport(campaign.value.campaign ? campaign.value : null)
      } else {
        setCampaignReport(null)
        setCampaignFailed(true)
      }
    })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const retry = () => {
    setAllFailed(false)
    setCampaignFailed(false)
    setAllReport(null)
    setCampaignReport(undefined)
    setAttempt((n) => n + 1)
  }

  const campaignAvailable = campaignReport !== null
  // 진행 중인 캠페인이 없으면 ?scope=CAMPAIGN으로 들어와도 월계1동 전체를 보여준다
  const scope: ReportScope = searchParams.get('scope') === 'CAMPAIGN' && campaignAvailable ? 'CAMPAIGN' : 'ALL'
  const current = scope === 'ALL' ? allReport : (campaignReport ?? null)
  const failed = scope === 'ALL' && allFailed

  return (
    <div className="flex min-h-0 flex-1 flex-col pb-[22px]">
      <div className="px-5 pt-5 pb-0.5">
        <h1 className="font-hand text-[28px] leading-[1.2] font-bold text-label">탄소절감 리포트</h1>
        <p className="mt-1 text-[13px] font-medium text-label-alt">이웃이 물건을 나눌수록 동네 나무가 자라요</p>
      </div>

      <div className="mx-5 mt-3.5 mb-1 flex gap-1 rounded-[14px] bg-sunken p-1" role="tablist">
        {SCOPE_TABS.map((tab) => {
          const selected = scope === tab.value
          const disabled = tab.value === 'CAMPAIGN' && !campaignAvailable
          return (
            <button
              key={tab.value}
              type="button"
              role="tab"
              aria-selected={selected}
              disabled={disabled}
              onClick={() => setSearchParams(tab.value === 'ALL' ? {} : { scope: tab.value }, { replace: true })}
              className={`flex-1 rounded-[11px] py-[9px] text-[14px] disabled:cursor-default disabled:text-ink-disabled ${
                selected ? 'bg-surface font-bold text-label' : 'font-semibold text-label-alt'
              }`}
            >
              {tab.label}
            </button>
          )
        })}
      </div>
      {!campaignAvailable && (
        <p className="mx-5 mt-1 text-[12px] font-medium text-label-alt">
          {campaignFailed ? '이번 캠페인 정보를 불러오지 못했어요' : '진행 중인 캠페인이 없어요'}
        </p>
      )}

      {failed ? <ReportError onRetry={retry} /> : current ? <ReportBody report={current} /> : <ReportSkeleton />}
    </div>
  )
}

function ReportBody({ report }: { report: CarbonReport }) {
  const { campaign } = report
  const isCampaign = report.scope === 'CAMPAIGN'

  return (
    <>
      {campaign ? (
        <>
          <CampaignSummaryCard
            campaign={campaign}
            reusedCount={report.reusedCount}
            carbonReductionKg={report.carbonReductionKg}
          />
          <SectionTitle title="날짜별 거래" caption="하루에 몇 개의 물건이 새 주인을 찾았을까요" className="px-5 pt-[22px] pb-1.5" />
          <DailyTradeChart startDate={campaign.startDate} endDate={campaign.endDate} dailyTrend={report.dailyTrend} />
        </>
      ) : (
        <TreeLevelCard since={report.since} reusedCount={report.reusedCount} carbonReductionKg={report.carbonReductionKg} />
      )}

      <div className="mt-5 h-2 flex-none bg-sunken" />

      <SectionTitle
        title="어떤 물건이 나무를 키웠을까?"
        caption={isCampaign ? '이번 캠페인 기준 (예상치)' : '누적 기준 (예상치)'}
        className="px-5 pt-5 pb-3"
      />
      <CategoryTreeList categoryBreakdown={report.categoryBreakdown} />

      {/* 로그인 시에만 — 내가 등록해서(나눔) 거래 완료된 물품 기준 */}
      {report.myCarbonReductionKg !== null && (
        <MyTreeCard carbonReductionKg={report.myCarbonReductionKg} campaign={isCampaign} />
      )}
    </>
  )
}

function SectionTitle({ title, caption, className }: { title: string; caption: string; className: string }) {
  return (
    <div className={className}>
      <h2 className="font-hand text-[22px] leading-[1.3] font-bold text-label">{title}</h2>
      <p className="mt-0.5 text-[13px] font-medium text-label-alt">{caption}</p>
    </div>
  )
}

function ReportError({ onRetry }: { onRetry: () => void }) {
  return (
    <div role="alert" className="mx-5 mt-3 flex flex-col items-center rounded-3xl border border-border bg-surface px-5 py-10 text-center">
      <p className="text-[15px] font-bold text-label">리포트를 불러오지 못했어요</p>
      <p className="mt-1 text-[13px] font-medium text-label-alt">잠시 후 다시 시도해 주세요</p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-4 cursor-pointer rounded-full border border-border bg-screen px-4 py-2 text-[13px] font-bold text-accent"
      >
        다시 불러오기
      </button>
    </div>
  )
}

function ReportSkeleton() {
  return (
    <div aria-busy="true" aria-label="리포트를 불러오는 중" className="animate-pulse">
      <div className="mx-5 mt-3 h-[420px] rounded-3xl bg-sunken" />
      <div className="mx-5 mt-7 h-6 w-48 rounded-md bg-sunken" />
      <div className="mx-5 mt-4 flex flex-col gap-3.5">
        {Array.from({ length: 5 }, (_, i) => (
          <div key={i} className="h-9 rounded-xl bg-sunken" />
        ))}
      </div>
    </div>
  )
}
