import { Link } from 'react-router-dom'
import { treesFromKg } from '../../lib/carbonTree'
import MaterialIcon from '../icons/MaterialIcon'
import { formatNumber } from './format'

interface MyTreeCardProps {
  carbonReductionKg: number
  /** 캠페인 탭이면 "이번 캠페인에서 …" 문구 */
  campaign: boolean
}

/** ④ 내가 키운 나무 (로그인 시에만). 카드 전체가 마이페이지 링크다. */
export default function MyTreeCard({ carbonReductionKg, campaign }: MyTreeCardProps) {
  const trees = formatNumber(treesFromKg(carbonReductionKg))

  return (
    <Link
      to="/mypage"
      className="mx-5 mt-[18px] flex items-center gap-3 rounded-2xl bg-amber-tint px-4 py-3.5 text-terracotta-deep"
    >
      <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-surface text-terracotta">
        <MaterialIcon name="potted_plant" size={20} />
      </span>
      <div className="min-w-0 flex-1">
        <div className={`text-[14px] font-bold whitespace-nowrap ${campaign ? 'tracking-[-0.02em]' : ''}`}>
          {campaign ? `이번 캠페인에서 내가 키운 나무는 ${trees}그루예요` : `내가 키운 나무는 ${trees}그루예요`}
        </div>
        <div className="mt-0.5 text-[12px] font-medium">
          {formatNumber(carbonReductionKg)}kg CO₂e · 마이페이지에서 자세히 보기
        </div>
      </div>
      <MaterialIcon name="chevron_right" size={18} color="var(--color-terracotta-ink)" />
    </Link>
  )
}
