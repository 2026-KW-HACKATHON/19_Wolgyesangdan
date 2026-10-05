import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyApplications, type ApplicationStatus, type MyApplicationSummary } from '../../api/applications'
import { ApiError } from '../../api/client'
import { getReservation, type ReservationDetail, type ReservationStatus } from '../../api/reservations'
import { isLoggedIn } from '../../lib/authStorage'
import { TRADE_METHOD_LABEL } from '../../lib/item'
import MaterialIcon from '../icons/MaterialIcon'

/** 물품 하나에 받는 신청 정원 — 대기 순번 바의 칸 수 (백엔드 Item.MAX_APPLICANTS) */
const MAX_APPLICANTS = 5
/** 한 번에 받아 오는 신청 수 */
const PAGE_SIZE = 50

const STATUS_BADGE: Record<ApplicationStatus, { label: string; className: string }> = {
  WAITING: { label: '배정 중', className: 'bg-amber-badge text-amber-badge-ink' },
  SELECTED: { label: '나에게 배정됨', className: 'bg-primary text-screen' },
  COMPLETED: { label: '전달 완료', className: 'bg-sunken text-ink-2' },
  CANCELED: { label: '신청 취소됨', className: 'bg-sunken text-ink-2' },
}

const RESERVATION_STATUS_LABEL: Record<ReservationStatus, string> = {
  SCHEDULED: '약속 예정',
  HUB_DROP_SCHEDULED: '거점 입고 예정',
  HUB_RECEIVED: '거점 보관 중',
  PICKUP_SCHEDULED: '수령 예정',
  RECONFIRMED: '수령 확정',
  COMPLETED: '전달 완료',
  NO_SHOW: '미수령',
  CANCELED: '거래 취소',
}

function formatMonthDay(isoDateTime: string) {
  const date = new Date(isoDateTime)
  return `${date.getMonth() + 1}.${date.getDate()}`
}

/** "9.24 17:00" */
function formatDateTime(isoDateTime: string) {
  const date = new Date(isoDateTime)
  return `${formatMonthDay(isoDateTime)} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

/** 대기 순번 5칸 바. 내 순번 이전 칸은 진하게, 내 칸은 중간 톤으로 표시한다. */
function WaitlistBar({ myNo, total }: { myNo: number; total: number }) {
  return (
    <div className="mt-2.5 flex gap-1">
      {Array.from({ length: total }, (_, i) => {
        const cls =
          i < myNo - 1 ? 'bg-primary' : i === myNo - 1 ? 'bg-primary-mid' : 'bg-surface'
        return <span key={i} className={`h-[7px] flex-1 rounded-full ${cls}`} />
      })}
    </div>
  )
}

/** 상대가 공개하기로 고른 연락 수단 하나 */
function ContactTile({ counterpart }: { counterpart: ReservationDetail['counterpart'] }) {
  if (counterpart.contactType === 'OPENCHAT' && counterpart.openchatLink) {
    return (
      <div className="mt-2.5 flex items-center gap-[9px] rounded-[14px] border border-border bg-surface px-3.5 py-3">
        <span className="flex size-[34px] flex-none items-center justify-center rounded-[11px] bg-kakao text-kakao-ink">
          <MaterialIcon name="chat_bubble" size={18} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-[14px] font-bold text-label">카카오 오픈채팅방</div>
          <div className="truncate text-[12px] font-medium text-label-alt">
            {counterpart.openchatLink.replace(/^https?:\/\//, '')}
          </div>
        </div>
        <a
          href={counterpart.openchatLink}
          target="_blank"
          rel="noreferrer"
          className="flex-none rounded-[10px] bg-primary px-2.5 py-2 text-[12px] font-bold text-screen"
        >
          열기
        </a>
      </div>
    )
  }
  if (counterpart.contactType === 'PHONE' && counterpart.phone) {
    return (
      <div className="mt-2.5 flex items-center gap-[9px] rounded-[14px] border border-border bg-surface px-3.5 py-3">
        <MaterialIcon name="call" size={18} className="text-accent" />
        <a href={`tel:${counterpart.phone}`} className="flex-1 text-[14px] font-bold text-label">
          {counterpart.phone}
        </a>
      </div>
    )
  }
  return null
}

/**
 * 배정된 신청의 예약 정보 (GET /applications/{applicationId}/reservation).
 * 거래 방식·약속 일시·진행 상태와, 등록자가 공개한 연락 수단을 보여준다.
 */
function ReservationSection({ application }: { application: MyApplicationSummary }) {
  // undefined = 받는 중, null = 예약이 아직 없음(배정 직후 등)
  const [reservation, setReservation] = useState<ReservationDetail | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    getReservation(application.id)
      .then((detail) => {
        if (!ignore) setReservation(detail)
      })
      .catch((e: unknown) => {
        if (ignore) return
        if (e instanceof ApiError && e.status === 404) setReservation(null)
        else setError(e instanceof ApiError ? e.message : '예약 정보를 불러오지 못했어요.')
      })
    return () => {
      ignore = true
    }
  }, [application.id])

  // 노쇼·취소된 예약은 서버가 연락 수단을 내려주지 않는다
  const broken = reservation?.status === 'NO_SHOW' || reservation?.status === 'CANCELED'
  const hasContact = Boolean(reservation?.counterpart.phone || reservation?.counterpart.openchatLink)

  return (
    <>
      <div className="mt-3 flex items-center gap-2 rounded-[14px] bg-primary-tint px-3.5 py-3">
        <MaterialIcon name={broken ? 'error' : 'check_circle'} size={19} className="flex-none text-accent" />
        <span className="text-[13px] font-semibold text-primary-tint-ink">
          {broken
            ? '거래가 이어지지 않아 연락 수단이 더 이상 공개되지 않아요.'
            : application.waitlistRank
              ? `대기 ${application.waitlistRank}번이었어요. 등록자와 약속을 잡아주세요.`
              : '등록자와 약속을 잡아주세요.'}
        </span>
      </div>

      {reservation && (
        <div className="mt-2.5 rounded-[14px] border border-border bg-surface px-3.5 py-[13px]">
          <div className="flex items-center gap-2">
            <span className="text-[12px] font-bold text-label-alt">
              {TRADE_METHOD_LABEL[reservation.tradeMethod]}
              {reservation.scheduledAt && ` · ${formatDateTime(reservation.scheduledAt)} 약속`}
            </span>
            <span className="ml-auto flex-none rounded-[7px] bg-sunken px-[7px] py-[3px] text-[11px] font-bold text-ink-2">
              {RESERVATION_STATUS_LABEL[reservation.status]}
            </span>
          </div>
          {hasContact && (
            <>
              <div className="mt-2.5 text-[12px] font-bold text-label-alt">
                {reservation.counterpart.nickname} 님이 선택한 연락 수단
              </div>
              <ContactTile counterpart={reservation.counterpart} />
            </>
          )}
          {!hasContact && !broken && (
            <p className="mt-2 text-[12px] font-medium text-label-alt">
              {reservation.counterpart.nickname} 님이 아직 연락 수단을 등록하지 않았어요.
            </p>
          )}
        </div>
      )}

      {reservation === undefined && !error && (
        <p className="mt-2.5 text-[12px] font-medium text-label-alt">예약 정보를 불러오는 중이에요…</p>
      )}
      {reservation === null && (
        <p className="mt-2.5 text-[12px] font-medium text-label-alt">약속 정보가 준비되면 여기에 보여드려요.</p>
      )}
      {error && (
        <p role="alert" className="mt-2.5 text-[12px] font-semibold text-terracotta">
          {error}
        </p>
      )}
    </>
  )
}

/** 마이페이지 "내가 신청한 물품" 탭 (GET /users/me/applications). */
export default function AppliedList() {
  const navigate = useNavigate()
  const loggedIn = isLoggedIn()
  const [applications, setApplications] = useState<MyApplicationSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [hiddenIds, setHiddenIds] = useState<number[]>([])

  useEffect(() => {
    if (!loggedIn) return
    let ignore = false
    getMyApplications(0, PAGE_SIZE)
      .then((page) => {
        if (!ignore) setApplications(page.content)
      })
      .catch((e: unknown) => {
        if (!ignore) setError(e instanceof ApiError ? e.message : '신청한 물품을 불러오지 못했어요.')
      })
    return () => {
      ignore = true
    }
  }, [loggedIn])

  const handleCancel = (id: number) => {
    // TODO: DELETE /applications/:id 연동. 취소하면 뒤 순번이 자동으로 당겨진다.
    if (!window.confirm('신청을 취소할까요?')) return
    setHiddenIds((prev) => [...prev, id])
  }

  if (!loggedIn) {
    return <p className="px-5 py-10 text-center text-[14px] font-medium text-label-alt">로그인하면 신청한 물품을 볼 수 있어요</p>
  }
  if (error) {
    return (
      <p role="alert" className="px-5 py-10 text-center text-[14px] font-semibold text-terracotta">
        {error}
      </p>
    )
  }
  if (!applications) {
    return <p className="px-5 py-10 text-center text-[14px] font-medium text-label-alt">신청한 물품을 불러오는 중이에요…</p>
  }

  const items = applications.filter((a) => !hiddenIds.includes(a.id))
  if (items.length === 0) {
    return <p className="px-5 py-10 text-center text-[14px] font-medium text-label-alt">아직 신청한 물품이 없어요</p>
  }

  return (
    <div className="flex flex-col gap-3 px-5">
      {items.map((item) => {
        const selected = item.status === 'SELECTED'
        // 끝난 신청(전달 완료·취소)은 흐리게 두고 버튼을 보여주지 않는다
        const closed = item.status === 'COMPLETED' || item.status === 'CANCELED'
        const badge = STATUS_BADGE[item.status]
        return (
          <div
            key={item.id}
            className={`rounded-[20px] bg-surface p-3.5 ${
              selected ? 'border-2 border-primary' : 'border border-border'
            } ${closed ? 'opacity-85' : ''}`}
          >
            <div className="flex gap-3">
              <span className="flex size-[72px] flex-none items-center justify-center rounded-2xl bg-primary-tint text-accent">
                <MaterialIcon name="inventory_2" size={30} />
              </span>
              <div className="min-w-0 flex-1">
                <span className={`inline-block rounded-[7px] px-[7px] py-[3px] text-[11px] font-bold ${badge.className}`}>
                  {badge.label}
                </span>
                <div className="mt-1.5 truncate text-[16px] font-bold text-label">{item.itemName}</div>
                <div className="mt-0.5 text-[13px] font-medium text-label-alt">{formatMonthDay(item.appliedAt)} 신청</div>
              </div>
            </div>

            {item.status === 'WAITING' && item.waitlistRank && (
              <div className="mt-3 rounded-[14px] bg-primary-tint px-3.5 py-[13px]">
                <div className="flex items-baseline gap-1.5">
                  <span className="text-[14px] font-bold text-body">대기</span>
                  <span className="text-[26px] font-extrabold tracking-[-0.02em] text-primary-dark">{item.waitlistRank}</span>
                  <span className="text-[15px] font-bold text-primary-dark">번</span>
                  <span className="ml-auto text-[13px] font-semibold text-primary-tint-ink">
                    대기는 {MAX_APPLICANTS}번까지
                  </span>
                </div>
                <WaitlistBar myNo={item.waitlistRank} total={MAX_APPLICANTS} />
                <p className="mt-2.5 text-[12px] leading-normal font-medium text-primary-tint-ink">
                  {item.waitlistRank >= MAX_APPLICANTS
                    ? '대기는 5번까지만 받아요. 마지막 순번이라 배정이 어려울 수 있어요.'
                    : '앞 순번이 취소하면 자동으로 올라가요. 우선배정 인증을 하면 순번이 앞당겨질 수 있어요.'}
                </p>
              </div>
            )}

            {selected && <ReservationSection application={item} />}

            {!closed && (
              <div className="mt-2.5 flex gap-2">
                {item.status === 'WAITING' && (
                  <button
                    type="button"
                    onClick={() => handleCancel(item.id)}
                    className="flex-1 cursor-pointer rounded-xl border border-border py-2.5 text-[13px] font-bold text-ink-2"
                  >
                    신청 취소
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => navigate(`/items/${item.itemId}`)}
                  className="flex-1 cursor-pointer rounded-xl bg-primary py-2.5 text-[13px] font-bold text-screen"
                >
                  물품 보기
                </button>
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
