import type { CategoryCarbon, CategoryGroup } from '../../data/carbonReport'
import { treesFromKg } from '../../lib/carbonTree'
import MaterialIcon from '../icons/MaterialIcon'
import { formatNumber } from './format'

const CATEGORY_TILE: Record<CategoryGroup, { icon: string; bg: string; fg: string }> = {
  가구: { icon: 'chair', bg: '#F2E5D3', fg: '#A96B33' },
  가전: { icon: 'bolt', bg: '#E7EBD8', fg: '#4F6B2A' },
  주방: { icon: 'local_cafe', bg: '#EFE7D4', fg: '#806528' },
  생활: { icon: 'home', bg: '#E5E9DA', fg: '#5B6B3A' },
  기타: { icon: 'more_horiz', bg: '#EBE4D1', fg: '#4A4A40' },
}

/** 1위(약 39%)가 바의 86% 정도를 채우도록 확대하는 배율 */
const BAR_SCALE = 2.2

/** ③ 어떤 물건이 나무를 키웠을까? — 값 내림차순 카테고리 행 */
export default function CategoryTreeList({ categoryBreakdown }: { categoryBreakdown: CategoryCarbon[] }) {
  const rows = [...categoryBreakdown].sort((a, b) => b.carbonReductionKg - a.carbonReductionKg)

  return (
    <ul className="flex flex-col gap-3.5 px-5">
      {rows.map((row, i) => {
        const tile = CATEGORY_TILE[row.categoryGroup]
        const percent = Math.round(row.ratio * 100)
        return (
          <li key={row.categoryGroup} className="flex items-center gap-3">
            <span
              className="flex size-9 flex-none items-center justify-center rounded-xl"
              style={{ background: tile.bg, color: tile.fg }}
            >
              <MaterialIcon name={tile.icon} size={19} />
            </span>
            <div className="flex min-w-0 flex-1 flex-col gap-1.5">
              <div className="flex items-baseline justify-between">
                <span className="text-[14px] font-bold text-label">{row.categoryGroup}</span>
                <span className="text-[13px] font-semibold text-label-alt">
                  나무 {formatNumber(treesFromKg(row.carbonReductionKg))}그루 · {percent}%
                </span>
              </div>
              <div className="h-2.5 rounded-full bg-sunken">
                <div
                  // 0에서 제 값까지 자라고, 아래 행일수록 조금씩 늦게 시작한다
                  className="h-full max-w-full animate-bar-grow-x rounded-full bg-primary motion-reduce:animate-none"
                  style={{ width: `${Math.min(row.ratio * 100 * BAR_SCALE, 100)}%`, animationDelay: `${i * 0.08}s` }}
                />
              </div>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
