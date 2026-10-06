import { useState } from 'react'
import type { DailyTrade } from '../../data/carbonReport'
import MaterialIcon from '../icons/MaterialIcon'
import { formatMonthDay } from './format'

const MAX_BAR_PX = 84
/** 칸이 이보다 많으면 막대 위 숫자는 최다일·오늘만 보여준다 (좁은 칸에서 숫자가 겹침) */
const MAX_LABELED_CELLS = 20

interface DayCell {
  date: string
  /** 남은 날이면 null */
  count: number | null
}

function toDateString(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** startDate ~ endDate 전체 일수만큼 칸을 만들고, 거래 기록이 없는 날(남은 날)은 count를 비운다. */
function buildCells(startDate: string, endDate: string, dailyTrend: DailyTrade[]): DayCell[] {
  const counts = new Map(dailyTrend.map((d) => [d.date, d.count]))
  const cells: DayCell[] = []
  const end = new Date(`${endDate}T00:00:00`)
  for (let d = new Date(`${startDate}T00:00:00`); d <= end; d.setDate(d.getDate() + 1)) {
    const date = toDateString(d)
    cells.push({ date, count: counts.get(date) ?? null })
  }
  return cells
}

interface DailyTradeChartProps {
  startDate: string
  endDate: string
  dailyTrend: DailyTrade[]
}

/** 5c ②′ 날짜별 거래 차트 + 최다 거래일 인사이트 */
export default function DailyTradeChart({ startDate, endDate, dailyTrend }: DailyTradeChartProps) {
  const [selected, setSelected] = useState<string | null>(null)
  const cells = buildCells(startDate, endDate, dailyTrend)
  const crowded = cells.length > MAX_LABELED_CELLS
  const todayDate = toDateString(new Date())
  const maxCount = Math.max(1, ...dailyTrend.map((d) => d.count))
  // 거래가 한 건도 없으면 최다 거래일도 없다 — 0개인 날을 "가장 많은 날"로 강조하지 않는다
  const peak = dailyTrend.reduce<DailyTrade | null>(
    (best, d) => (d.count > 0 && (!best || d.count > best.count) ? d : best),
    null,
  )
  const started = dailyTrend.length > 0

  return (
    <>
      <div className="mx-5 mt-1 rounded-[20px] border border-border bg-surface px-3 pt-3.5 pb-3">
        <div className="flex h-[124px] items-end gap-[3px]">
          {cells.map((cell, i) => {
            const isPeak = cell.date === peak?.date
            const isToday = cell.date === todayDate
            const isRemaining = cell.count === null
            // x축 날짜는 시작일 · 오늘 · 종료일 3개만 (칸이 좁아 모두 쓰면 겹침)
            const showDate = i === 0 || i === cells.length - 1 || isToday
            const barColor = isPeak ? 'bg-primary' : isToday ? 'bg-[#7E9A55]' : 'bg-primary-soft'
            return (
              <button
                key={cell.date}
                type="button"
                disabled={isRemaining}
                onClick={() => setSelected(selected === cell.date ? null : cell.date)}
                aria-label={isRemaining ? `${formatMonthDay(cell.date)} 남은 날` : `${formatMonthDay(cell.date)} ${cell.count}개`}
                className="relative flex min-w-0 flex-1 flex-col items-center gap-[5px] disabled:cursor-default"
              >
                {selected === cell.date && !isRemaining && (
                  <span className="absolute -top-6 z-10 rounded-md bg-label px-1.5 py-[3px] text-[10px] font-bold whitespace-nowrap text-screen">
                    {formatMonthDay(cell.date)} · {cell.count}개
                  </span>
                )}
                <span
                  className={`h-3 text-[10px] leading-3 font-bold ${isPeak ? 'text-primary-dark' : 'text-label-alt'}`}
                >
                  {crowded && !isPeak && !isToday ? '' : (cell.count ?? '')}
                </span>
                {isRemaining ? (
                  <span className="box-border h-2 w-full max-w-3.5 rounded-[5px] border-[1.5px] border-dashed border-border-deep" />
                ) : (
                  <span
                    // 0에서 제 높이까지 자라고, 왼쪽 날짜부터 차례로 올라온다
                    className={`w-full max-w-3.5 animate-bar-grow-y rounded-[5px] motion-reduce:animate-none ${barColor}`}
                    style={{ height: ((cell.count ?? 0) / maxCount) * MAX_BAR_PX, animationDelay: `${i * 0.03}s` }}
                  />
                )}
                <span
                  className={`h-3 text-[10px] leading-3 whitespace-nowrap ${
                    isToday ? 'font-extrabold text-accent' : 'font-medium text-label-alt'
                  }`}
                >
                  {showDate ? formatMonthDay(cell.date) : ''}
                </span>
              </button>
            )
          })}
        </div>
        <div className="mt-2.5 flex flex-wrap gap-3.5 px-1">
          {peak && <Legend swatch="bg-primary">가장 많은 날</Legend>}
          <Legend swatch="bg-[#7E9A55]">오늘</Legend>
          <Legend swatch="box-border border-[1.5px] border-dashed border-border-deep">남은 기간</Legend>
        </div>
      </div>
      {started && (
        <div className="mx-5 mt-2.5 flex items-center gap-2 rounded-xl bg-amber-tint px-3 py-2.5">
          <MaterialIcon name="calendar_month" size={18} color="var(--color-terracotta-ink)" />
          <span className="text-[13px] font-semibold text-terracotta-deep">
            {peak
              ? `${formatMonthDay(peak.date)}에 ${peak.count}개로 가장 많이 거래됐어요`
              : '아직 거래가 완료된 물품이 없어요'}
          </span>
        </div>
      )}
    </>
  )
}

function Legend({ swatch, children }: { swatch: string; children: string }) {
  return (
    <span className="inline-flex items-center gap-[5px] text-[11px] font-semibold text-ink-2">
      <span className={`size-2.5 rounded-[3px] ${swatch}`} />
      {children}
    </span>
  )
}
