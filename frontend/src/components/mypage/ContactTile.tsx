import type { ReservationDetail } from '../../api/reservations'
import MaterialIcon from '../icons/MaterialIcon'

/** 거래 상대가 공개하기로 고른 연락 수단 하나 (오픈채팅 링크 또는 전화번호). 공개된 값이 없으면 그리지 않는다. */
export default function ContactTile({ counterpart }: { counterpart: ReservationDetail['counterpart'] }) {
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
