import { useNavigate } from 'react-router-dom'
import { useTodo } from '../contexts/TodoContext'
import { formatDateTime } from '../lib/reservation'
import MaterialIcon from './icons/MaterialIcon'

/**
 * 홈 상단 "할 일" 배너 (#188) — 배정됐는데 놓치기 쉬운 일 중 가장 급한 것 하나.
 * 신청자의 수령 재확인(기한 있음)을 등록자의 전달보다 먼저 보여준다. 할 일이 없으면 아무것도 그리지 않는다.
 */
export default function TodoBanner() {
  const navigate = useNavigate()
  const { todo } = useTodo()
  const total = todo.reconfirms.length + todo.deliveries.length
  if (total === 0) return null

  const reconfirm = todo.reconfirms[0]
  const delivery = todo.deliveries[0]
  const banner = reconfirm
    ? {
        title: `‘${reconfirm.itemName}’ 배정됐어요`,
        detail: reconfirm.reconfirmationDeadline
          ? `${formatDateTime(reconfirm.reconfirmationDeadline)}까지 수령을 재확인해 주세요`
          : '받으러 갈 수 있다면 수령을 재확인해 주세요',
        to: '/mypage?tab=applied',
      }
    : {
        title: `‘${delivery.itemName}’ 배정됐어요`,
        detail: delivery.reconfirmed
          ? delivery.tradeMethod === 'DIRECT'
            ? "신청자가 수령을 확인했어요 · 건넨 뒤 '전달 완료'를 눌러 주세요"
            : '신청자가 수령을 확인했어요 · 거점에 맡겨 주세요'
          : '신청자와 연락해 전달 약속을 잡아 주세요',
        to: '/mypage',
      }

  return (
    <button
      type="button"
      onClick={() => navigate(banner.to)}
      className="mx-5 flex items-center gap-3 rounded-2xl border border-[#E8C9A8] bg-amber-tint px-4 py-3.5 text-left"
    >
      <span className="flex size-10 flex-none items-center justify-center rounded-xl bg-surface text-terracotta">
        <MaterialIcon name="notifications_active" size={22} />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-1.5">
          <span className="truncate text-[15px] font-bold text-terracotta-deep">{banner.title}</span>
          {total > 1 && (
            <span className="flex-none rounded-[7px] bg-terracotta px-1.5 py-0.5 text-[11px] font-bold text-screen">
              외 {total - 1}건
            </span>
          )}
        </span>
        <span className="mt-0.5 block text-[12px] font-medium text-terracotta-deep opacity-85">{banner.detail}</span>
      </span>
      <MaterialIcon name="chevron_right" size={20} className="flex-none text-terracotta" />
    </button>
  )
}
