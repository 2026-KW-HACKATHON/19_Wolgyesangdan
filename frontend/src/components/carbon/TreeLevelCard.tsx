import { TREE_LEVELS, treeProgress, withSubjectParticle } from '../../lib/carbonTree'
import MaterialIcon from '../icons/MaterialIcon'
import { formatNumber, formatYearMonth } from './format'
import MetricPair from './MetricPair'
import TreeScene from './TreeScene'

interface TreeLevelCardProps {
  /** 누적 집계 시작 월 (YYYY-MM) */
  since: string
  reusedCount: number
  co2eTotalKg: number
}

/** 5b ① 동네 나무 카드. 누적 절감량에 따라 장면이 5d의 레벨 5단계로 바뀐다. */
export default function TreeLevelCard({ since, reusedCount, co2eTotalKg }: TreeLevelCardProps) {
  const { level, next, remainKg, progress, fruits } = treeProgress(co2eTotalKg, reusedCount)

  return (
    <section className="mx-5 mt-3 overflow-hidden rounded-3xl border border-border bg-surface">
      <div className="relative h-[236px] overflow-hidden bg-[#EEF1E0]">
        <TreeScene level={level.lv} fruits={fruits} />
        <span className="absolute top-3.5 left-4 z-10 inline-flex items-center gap-1 rounded-full bg-primary px-[11px] py-[5px] text-[12px] font-bold text-screen">
          <MaterialIcon name="park" size={14} />
          Lv.{level.lv} {level.name}
        </span>
        <span className="absolute top-4 right-4 z-10 text-[12px] font-semibold text-[#57603F]">
          {formatYearMonth(since)}부터
        </span>
        {fruits > 0 && (
          <span className="absolute right-3 bottom-2 z-10 rounded-lg bg-surface px-2 py-[3px] text-[11px] font-bold text-terracotta-deep">
            열매 1개 = 물품 100개
          </span>
        )}
      </div>

      <div className="px-[18px] pt-4 pb-[18px]">
        <MetricPair countLabel="함께 나눈 물품" count={reusedCount} co2eKg={co2eTotalKg} />

        <div className="mt-4 flex items-baseline justify-between">
          <span className="text-[13px] font-bold text-body">
            {next ? `${withSubjectParticle(next.name)} 되기까지` : level.message}
          </span>
          <span className="text-[13px] font-bold text-accent">
            {next ? `${formatNumber(remainKg)}kg 남았어요` : '다음 캠페인에서 숲을 넓혀봐요'}
          </span>
        </div>
        <div
          className="mt-2 h-2.5 overflow-hidden rounded-full bg-sunken"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(progress * 100)}
          aria-label={next ? `${next.name}까지 진행률` : '최고 레벨 달성'}
        >
          <div
            className="h-full animate-bar-grow-x rounded-full bg-primary motion-reduce:animate-none"
            style={{ width: `${progress * 100}%` }}
          />
        </div>

        <ol className="mt-3 flex">
          {TREE_LEVELS.map((l) => {
            const reached = l.lv <= level.lv
            const current = l.lv === level.lv
            return (
              <li key={l.lv} className="flex flex-1 flex-col items-center gap-[5px]">
                <span
                  className={`box-border rounded-full border-2 ${current ? 'size-4' : 'size-3'} ${
                    reached ? 'border-primary bg-primary' : 'border-border-deep bg-surface'
                  }`}
                />
                <span
                  className={`text-[11px] ${
                    current
                      ? 'font-extrabold text-primary-dark'
                      : reached
                        ? 'font-semibold text-ink-2'
                        : 'font-semibold text-label-alt'
                  }`}
                >
                  {l.name}
                </span>
              </li>
            )
          })}
        </ol>
      </div>
    </section>
  )
}
