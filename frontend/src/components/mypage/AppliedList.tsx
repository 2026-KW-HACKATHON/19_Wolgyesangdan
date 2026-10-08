import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  cancelApplication,
  getMyApplications,
  type ApplicationStatus,
  type MyApplicationSummary,
} from '../../api/applications'
import { ApiError } from '../../api/client'
import {
  RECONFIRMABLE_STATUSES,
  getReservation,
  reconfirmReservation,
  type ReservationDetail,
} from '../../api/reservations'
import { useTodo } from '../../contexts/TodoContext'
import { isLoggedIn } from '../../lib/authStorage'
import { TRADE_METHOD_LABEL } from '../../lib/item'
import { RESERVATION_STATUS_LABEL, formatDateTime, formatMonthDay } from '../../lib/reservation'
import MaterialIcon from '../icons/MaterialIcon'
import EmptyState from '../EmptyState'
import MyItemThumb from './MyItemThumb'
import ContactTile from './ContactTile'
import ConfirmDialog from '../ConfirmDialog'

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

/**
 * 배정된 신청의 예약 정보 (GET /applications/{applicationId}/reservation).
 * 거래 방식·약속 일시·진행 상태와, 등록자가 공개한 연락 수단을 보여준다.
 */
function ReservationSection({ application }: { application: MyApplicationSummary }) {
  // undefined = 받는 중, null = 예약이 아직 없음(배정 직후 등)
  const [reservation, setReservation] = useState<ReservationDetail | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)
  const { refreshTodo } = useTodo()
  const [reconfirming, setReconfirming] = useState(false)
  const [reconfirmError, setReconfirmError] = useState<string | null>(null)

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

  // 수령 재확인 (PATCH /reservations/{reservationId}/reconfirm) — 받으러 가겠다고 기한 안에 다시 확인한다
  const canReconfirm =
    reservation != null && RECONFIRMABLE_STATUSES.includes(reservation.status) && !reservation.reconfirmedAt

  const handleReconfirm = async () => {
    if (!reservation || reconfirming) return
    setReconfirming(true)
    setReconfirmError(null)
    try {
      const result = await reconfirmReservation(reservation.id)
      setReservation({ ...reservation, status: result.status, reconfirmedAt: result.reconfirmedAt })
      refreshTodo()
    } catch (e) {
      setReconfirmError(e instanceof ApiError ? e.message : '재확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setReconfirming(false)
    }
  }

  // 노쇼·취소된 예약은 서버가 연락 수단을 내려주지 않는다 (전달 완료된 신청은 이 영역을 아예 그리지 않는다)
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

          {canReconfirm && (
            <div className="mt-3 border-t border-border pt-3">
              <p className="text-[12px] leading-normal font-medium text-label-alt">
                {reservation.reconfirmationDeadline
                  ? `${formatDateTime(reservation.reconfirmationDeadline)}까지 수령을 재확인해 주세요. 기한이 지나면 다음 순번에게 넘어가요.`
                  : '받으러 갈 수 있다면 수령을 재확인해 주세요.'}
              </p>
              <button
                type="button"
                onClick={handleReconfirm}
                disabled={reconfirming}
                className="mt-2 w-full cursor-pointer rounded-xl bg-primary py-2.5 text-[13px] font-bold text-screen disabled:opacity-60"
              >
                {reconfirming ? '재확인 중…' : '수령 재확인하기'}
              </button>
              {reconfirmError && (
                <p role="alert" className="mt-2 text-[12px] font-semibold text-terracotta">
                  {reconfirmError}
                </p>
              )}
            </div>
          )}
          {reservation.reconfirmedAt && (
            <p className="mt-3 flex items-center gap-1 border-t border-border pt-3 text-[12px] font-semibold text-accent">
              <MaterialIcon name="check_circle" size={15} />
              {formatDateTime(reservation.reconfirmedAt)}에 수령을 재확인했어요
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
  // 목록을 다시 불러올 때마다 올린다 (취소 후 남은 신청의 대기 순번이 당겨지므로 서버 값으로 새로 받는다)
  const [reloadKey, setReloadKey] = useState(0)
  /** 취소 요청 중인 신청 id — 버튼을 잠근다 */
  const [cancelingId, setCancelingId] = useState<number | null>(null)
  const [cancelError, setCancelError] = useState<{ id: number; message: string } | null>(null)

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
  }, [loggedIn, reloadKey])

  // 취소할지 묻는 팝업을 띄운 신청 (#192)
  const [confirmCancelId, setConfirmCancelId] = useState<number | null>(null)

  const handleCancel = async (id: number) => {
    setConfirmCancelId(null)
    if (cancelingId !== null) return
    setCancelingId(id)
    setCancelError(null)
    try {
      await cancelApplication(id)
      setReloadKey((key) => key + 1)
    } catch (e) {
      setCancelError({
        id,
        message: e instanceof ApiError ? e.message : '신청을 취소하지 못했어요. 잠시 후 다시 시도해 주세요.',
      })
      // 그 사이 배정되는 등 상태가 바뀌었을 수 있어서 목록도 새로 받는다
      if (e instanceof ApiError && e.status === 409) setReloadKey((key) => key + 1)
    } finally {
      setCancelingId(null)
    }
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

  if (applications.length === 0) {
    return (
      <EmptyState
        title="아직 받아 온 물건이 없어요"
        description="이웃이 내놓은 물건을 구경해 보세요"
        action={{ label: '둘러보기', onClick: () => navigate('/browse') }}
      />
    )
  }

  return (
    <div className="flex flex-col gap-3 px-5">
      {applications.map((item) => {
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
              <MyItemThumb
                imageUrl={item.itemThumbnailImageUrl}
                alt={item.itemName}
                className="size-[72px] rounded-2xl"
                iconSize={30}
              />
              <div className="min-w-0 flex-1">
                <span className={`inline-block rounded-[7px] px-[7px] py-[3px] text-[11px] font-bold ${badge.className}`}>
                  {badge.label}
                </span>
                <div className="mt-1.5 truncate text-[16px] font-bold text-label">{item.itemName}</div>
                <div className="mt-0.5 text-[13px] font-medium text-label-alt">{formatMonthDay(item.appliedAt)} 신청</div>
              </div>
            </div>

            {/* 배정 전에는 서버가 순번을 내려주지 않는다 (#265) — 순번 대신 언제 알 수 있는지만 알려준다 */}
            {item.status === 'WAITING' && !item.waitlistRank && (
              <div className="mt-3 flex gap-2 rounded-[14px] bg-primary-tint px-3.5 py-[13px]">
                <MaterialIcon name="hourglass_top" size={18} className="mt-px flex-none text-accent" />
                <div>
                  <div className="text-[14px] font-bold text-primary-tint-ink">신청이 접수됐어요</div>
                  <p className="mt-1 text-[12px] leading-normal font-medium text-primary-tint-ink">
                    순번은 신청 마감 뒤 배정이 끝나면 알려드려요.
                  </p>
                </div>
              </div>
            )}

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
                    : '배정된 분이 받지 못하면 순번대로 넘어가요.'}
                </p>
              </div>
            )}

            {selected && <ReservationSection application={item} />}

            {!closed && (
              <div className="mt-2.5 flex gap-2">
                {item.status === 'WAITING' && (
                  <button
                    type="button"
                    onClick={() => setConfirmCancelId(item.id)}
                    disabled={cancelingId !== null}
                    className="flex-1 cursor-pointer rounded-xl border border-border py-2.5 text-[13px] font-bold text-ink-2 disabled:cursor-default disabled:opacity-60"
                  >
                    {cancelingId === item.id ? '취소하는 중…' : '신청 취소'}
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
            {cancelError?.id === item.id && (
              <p role="alert" className="mt-2 text-[12px] font-semibold text-terracotta">
                {cancelError.message}
              </p>
            )}
          </div>
        )
      })}
      {confirmCancelId !== null && (
        <ConfirmDialog
          icon="event_busy"
          title="신청을 취소할까요?"
          description="신청 마감 전이면 다시 신청할 수 있어요. 다시 신청하면 대기 순서는 맨 뒤가 돼요."
          confirmLabel="신청 취소"
          cancelLabel="그대로 둘게요"
          onConfirm={() => handleCancel(confirmCancelId)}
          onCancel={() => setConfirmCancelId(null)}
        />
      )}
    </div>
  )
}
