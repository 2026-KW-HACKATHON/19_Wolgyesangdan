import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { getMyItems } from '../../api/items'
import { isLoggedIn } from '../../lib/authStorage'
import { formatMonthDay } from '../../lib/reservation'
import type { ItemStatus, MyItemSummary } from '../../types/item'
import MaterialIcon from '../icons/MaterialIcon'
import EmptyState from '../EmptyState'
import MyItemThumb from './MyItemThumb'
import OwnerReservationSection from './OwnerReservationSection'

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

  // 전달 완료에 성공하면 다시 조회하지 않고 그 물품만 거래 완료로 바꾼다
  const markCompleted = (itemId: number) => {
    setItems((prev) => prev?.map((item) => (item.id === itemId ? { ...item, status: 'COMPLETED' } : item)) ?? prev)
  }

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
    return (
      <EmptyState
        title="보따리가 비어 있어요"
        description="안 쓰는 물건을 이웃에게 건네 보세요"
        action={{ label: '물품 등록하기', onClick: () => navigate('/register') }}
      />
    )
  }

  return (
    <div className="px-5">
      {items.map((item, i) => {
        const badge = STATUS_BADGE[item.status]
        // 배정된 물품만 예약 정보(신청자 연락 수단·전달 완료)를 보여준다. 완료 직후에도 결과가 보이도록 key로 유지한다
        const showReservation =
          item.applicationId !== null && (item.status === 'ASSIGNED' || item.status === 'COMPLETED')
        return (
          <div key={item.id}>
            <button
              type="button"
              onClick={() => navigate(`/items/${item.id}`)}
              className="flex w-full cursor-pointer items-center gap-3 py-3.5 text-left"
            >
              <MyItemThumb imageUrl={item.thumbnailImageUrl} alt={item.name} className="size-16 rounded-[14px]" iconSize={26} />
              <span className="min-w-0 flex-1">
                <span className={`inline-block rounded-[7px] px-[7px] py-[3px] text-[11px] font-bold ${badge.className}`}>
                  {badge.label}
                </span>
                <span className="mt-[5px] block truncate text-[15px] font-bold text-label">{item.name}</span>
                <span className="mt-px block text-[13px] font-medium text-label-alt">{metaText(item)}</span>
              </span>
              <MaterialIcon name="chevron_right" size={18} className="text-label-alt" />
            </button>
            {showReservation && item.applicationId !== null && (
              <OwnerReservationSection applicationId={item.applicationId} onCompleted={() => markCompleted(item.id)} />
            )}
            {i < items.length - 1 && <div className="h-px bg-border" />}
          </div>
        )
      })}
    </div>
  )
}
