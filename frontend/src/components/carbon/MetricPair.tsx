import { formatNumber } from './format'

interface MetricPairProps {
  /** 왼쪽 지표 라벨 — 동 전체 "함께 나눈 물품", 캠페인 "거래된 물품" */
  countLabel: string
  count: number
  co2eKg: number
}

/** 물품 수 / 줄인 탄소 2열 지표 (세로 구분선) */
export default function MetricPair({ countLabel, count, co2eKg }: MetricPairProps) {
  return (
    <div className="flex gap-3.5">
      <div className="flex-1">
        <div className="text-[12px] font-semibold text-label-alt">{countLabel}</div>
        <div className="mt-[3px] flex items-baseline gap-[3px] text-label">
          <span className="text-[26px] font-extrabold tracking-[-0.02em]">{formatNumber(count)}</span>
          <span className="text-[14px] font-bold">개</span>
        </div>
      </div>
      <div className="w-px bg-border" />
      <div className="flex-1">
        <div className="text-[12px] font-semibold text-label-alt">줄인 탄소 (예상치)</div>
        <div className="mt-[3px] flex items-baseline gap-[3px] text-primary-dark">
          <span className="text-[26px] font-extrabold tracking-[-0.02em]">{formatNumber(co2eKg)}</span>
          <span className="text-[14px] font-bold">kg</span>
        </div>
      </div>
    </div>
  )
}
