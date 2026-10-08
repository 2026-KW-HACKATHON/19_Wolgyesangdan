import { useEffect, useState } from 'react'
import { ApiError } from '../../api/client'
import { completeReservation, getReservation, type ReservationDetail } from '../../api/reservations'
import { TRADE_METHOD_LABEL } from '../../lib/item'
import { RESERVATION_STATUS_LABEL, formatDateTime } from '../../lib/reservation'
import MaterialIcon from '../icons/MaterialIcon'
import ContactTile from './ContactTile'
import { useTodo } from '../../contexts/TodoContext'
import ConfirmDialog from '../ConfirmDialog'

interface OwnerReservationSectionProps {
  /** 배정된 신청 id (GET /users/me/items 의 applicationId) */
  applicationId: number
  /** 전달 완료에 성공했을 때 — 목록의 물품 상태를 바꾸는 데 쓴다 */
  onCompleted: () => void
}

/**
 * 등록자가 보는 배정된 물품의 예약 정보 (GET /applications/{applicationId}/reservation).
 * 신청자가 공개한 연락 수단을 보여주고, 직거래는 건넨 뒤 "전달 완료"를 누른다
 * (PATCH /reservations/{reservationId}/complete).
 */
export default function OwnerReservationSection({ applicationId, onCompleted }: OwnerReservationSectionProps) {
  // undefined = 받는 중, null = 예약을 찾지 못함
  const { refreshTodo } = useTodo()
  const [reservation, setReservation] = useState<ReservationDetail | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)
  const [completing, setCompleting] = useState(false)
  const [completeError, setCompleteError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    getReservation(applicationId)
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
  }, [applicationId])

  // 되돌릴 수 없어서 확인 팝업을 먼저 띄운다 (#192)
  const [confirmOpen, setConfirmOpen] = useState(false)

  const handleComplete = async () => {
    setConfirmOpen(false)
    if (!reservation || completing) return
    setCompleting(true)
    setCompleteError(null)
    try {
      const result = await completeReservation(reservation.id)
      setReservation({ ...reservation, status: result.status })
      refreshTodo()
      onCompleted()
    } catch (e) {
      setCompleteError(e instanceof ApiError ? e.message : '전달 완료로 처리하지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setCompleting(false)
    }
  }

  if (error) {
    return (
      <p role="alert" className="pb-3.5 text-[12px] font-semibold text-terracotta">
        {error}
      </p>
    )
  }
  if (reservation === undefined) {
    return <p className="pb-3.5 text-[12px] font-medium text-label-alt">예약 정보를 불러오는 중이에요…</p>
  }
  if (reservation === null) return null

  // 노쇼·취소된 예약은 서버가 연락 수단을 내려주지 않는다
  const broken = reservation.status === 'NO_SHOW' || reservation.status === 'CANCELED'
  const completed = reservation.status === 'COMPLETED'
  const hasContact = Boolean(reservation.counterpart.phone || reservation.counterpart.openchatLink)
  const direct = reservation.tradeMethod === 'DIRECT'
  // 직거래이고 신청자가 수령을 재확인한 예약만 등록자가 완료할 수 있다
  const canComplete = direct && reservation.status === 'RECONFIRMED'

  return (
    <div className="mb-3.5 rounded-[14px] border border-border bg-surface px-3.5 py-[13px]">
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
            배정된 {reservation.counterpart.nickname} 님이 선택한 연락 수단
          </div>
          <ContactTile counterpart={reservation.counterpart} />
        </>
      )}
      {!hasContact && !broken && !completed && (
        <p className="mt-2 text-[12px] font-medium text-label-alt">
          배정된 {reservation.counterpart.nickname} 님이 아직 연락 수단을 등록하지 않았어요.
        </p>
      )}
      {broken && (
        <p className="mt-2 text-[12px] font-medium text-label-alt">
          거래가 이어지지 않아 연락 수단이 더 이상 공개되지 않아요.
        </p>
      )}

      {completed && (
        <p className="mt-3 flex items-center gap-1 border-t border-border pt-3 text-[12px] font-semibold text-accent">
          <MaterialIcon name="check_circle" size={15} />
          전달을 완료했어요
        </p>
      )}

      {!completed && !broken && (
        <div className="mt-3 border-t border-border pt-3">
          {direct ? (
            <>
              {!canComplete && (
                <p className="text-[12px] leading-normal font-medium text-label-alt">
                  신청자가 수령을 재확인하면 전달 완료를 누를 수 있어요.
                </p>
              )}
              <button
                type="button"
                onClick={() => setConfirmOpen(true)}
                disabled={!canComplete || completing}
                className={`w-full cursor-pointer rounded-xl bg-primary py-2.5 text-[13px] font-bold text-screen disabled:cursor-default disabled:opacity-50 ${
                  canComplete ? '' : 'mt-2'
                }`}
              >
                {completing ? '처리 중…' : '전달 완료'}
              </button>
            </>
          ) : (
            <p className="text-[12px] leading-normal font-medium text-label-alt">
              거점 거래는 운영진이 완료 처리해요.
            </p>
          )}
          {completeError && (
            <p role="alert" className="mt-2 text-[12px] font-semibold text-terracotta">
              {completeError}
            </p>
          )}
        </div>
      )}
      {confirmOpen && (
        <ConfirmDialog
          icon="volunteer_activism"
          tone="danger"
          title="물품을 건넸나요?"
          description="전달 완료로 처리하면 되돌릴 수 없어요. 신청자에게 물품을 건넨 뒤에 눌러 주세요."
          confirmLabel="전달 완료"
          cancelLabel="아직이에요"
          onConfirm={handleComplete}
          onCancel={() => setConfirmOpen(false)}
        />
      )}
    </div>
  )
}
