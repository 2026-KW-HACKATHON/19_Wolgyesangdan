import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { getMyItems } from '../../api/items'
import { isLoggedIn } from '../../lib/authStorage'
import type { ItemStatus, MyItemSummary } from '../../types/item'
import MaterialIcon from '../icons/MaterialIcon'

/** 한 번에 받아 오는 물품 수 */
const PAGE_SIZE = 50

/** 등록자 입장에서 보는 물품 상태 */
const STATUS_BADGE: Record<ItemStatus, { label: string; className: string }> = {
  REGISTERED: { label: '등록됨', className: 'bg-sunken text-ink-2' },
  OPEN: { label: '신청자 모집 중', className: 'bg-primary-tint text-primary-dark' },
  CLOSED: { label: '배정 중', className: 'bg-amber-badge text-amber-badge-ink' },
  ASSIGNED: { label: '배정 완료', className: 'bg-primary text-screen' },
  COMPLETED: { label: '전달 완료', className: 'bg-sunken text-ink-2' },
  CANCELED: { label: '취소됨', className: 'bg-sunken text-ink-2' },
}

function formatMonthDay(isoDateTime: string) {
  const date = new Date(isoDateTime)
  return `${date.getMonth() + 1}.${date.getDate()}`
}

/** 카드 둘째 줄 — 신청자 수와, 배정 뒤에는 전달 예정일(끝났으면 전달일) */
function metaText(item: MyItemSummary) {
  const parts = [`신청자 ${item.applicantCount}명`]
  if (item.scheduledAt) {
    parts.push(`${formatMonthDay(item.scheduledAt)} ${item.status === 'COMPLETED' ? '전달' : '전달 예정'}`)
  }
  return parts.join(' · ')
}

/** 마이페이지 "내가 등록한 물품" 탭 (GET /users/me/items). 상태와 관계없이 전부, 최근 등록순. */
export default function RegisteredList() {
  const navigate = useNavigate()
  const loggedIn = isLoggedIn()
  const [items, setItems] = useState<MyItemSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!loggedIn) return
    let ignore = false
    getMyItems(0, PAGE_SIZE)
      .then((page) => {
        if (!ignore) setItems(page.content)
      })
      .catch((e: unknown) => {
        if (!ignore) setError(e instanceof ApiError ? e.message : '등록한 물품을 불러오지 못했어요.')
      })
    return () => {
      ignore = true
    }
  }, [loggedIn])

  if (!loggedIn) {
    return <p className="px-5 py-10 text-center text-[14px] font-medium text-label-alt">로그인하면 등록한 물품을 볼 수 있어요</p>
  }
  if (error) {
    return (
      <p role="alert" className="px-5 py-10 text-center text-[14px] font-semibold text-terracotta">
        {error}
      </p>
    )
  }
  if (!items) {
    return <p className="px-5 py-10 text-center text-[14px] font-medium text-label-alt">등록한 물품을 불러오는 중이에요…</p>
  }
  if (items.length === 0) {
    return <p className="px-5 py-10 text-center text-[14px] font-medium text-label-alt">아직 등록한 물품이 없어요</p>
  }

  return (
    <div className="px-5">
      {items.map((item, i) => {
        const badge = STATUS_BADGE[item.status]
        return (
          <div key={item.id}>
            <button
              type="button"
              onClick={() => navigate(`/items/${item.id}`)}
              className="flex w-full cursor-pointer items-center gap-3 py-3.5 text-left"
            >
              <span className="flex size-16 flex-none items-center justify-center rounded-[14px] bg-primary-tint text-accent">
                <MaterialIcon name="inventory_2" size={26} />
              </span>
              <span className="min-w-0 flex-1">
                <span className={`inline-block rounded-[7px] px-[7px] py-[3px] text-[11px] font-bold ${badge.className}`}>
                  {badge.label}
                </span>
                <span className="mt-[5px] block truncate text-[15px] font-bold text-label">{item.name}</span>
                <span className="mt-px block text-[13px] font-medium text-label-alt">{metaText(item)}</span>
              </span>
              <MaterialIcon name="chevron_right" size={18} className="text-label-alt" />
            </button>
            {i < items.length - 1 && <div className="h-px bg-border" />}
          </div>
        )
      })}
    </div>
  )
}
