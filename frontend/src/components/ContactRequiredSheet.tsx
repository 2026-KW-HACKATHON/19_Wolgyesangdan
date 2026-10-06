import MaterialIcon from './icons/MaterialIcon'
import PrimaryButton from './PrimaryButton'

interface ContactRequiredSheetProps {
  onSetup: () => void
}

/**
 * 4a — 연락 수단이 없을 때 등록 폼 위에 올라오는 바텀시트.
 * 연락 수단은 등록의 필수 조건(서버도 403 ITEM_CONTACT_NOT_SET)이라 "나중에 하기"로 닫을 수 없다 —
 * 등록을 그만두려면 바텀 탭으로 다른 화면에 가면 되고, 작성 중인 내용은 임시 저장돼 있다.
 */
export default function ContactRequiredSheet({ onSetup }: ContactRequiredSheetProps) {
  return (
    <div className="absolute inset-0 z-10 flex flex-col justify-end bg-label/50">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="contact-sheet-title"
        className="rounded-t-3xl bg-screen px-[22px] pt-2 pb-[22px] shadow-[0_-12px_32px_rgba(31,36,25,0.22)]"
      >
        <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-border-strong" />

        <span className="flex size-13 items-center justify-center rounded-2xl bg-amber-tint text-amber-ink">
          <MaterialIcon name="phone_missed" size={26} />
        </span>

        <h2 id="contact-sheet-title" className="mt-3 font-hand text-[26px] leading-[1.25] font-bold text-label">
          연락 수단을 먼저 등록해주세요
        </h2>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">
          신청자가 배정되면 서로 연락이 닿아야 해요. 오픈채팅방 링크나 전화번호 중 하나만 등록하면 됩니다.
        </p>

        <div className="mt-3.5 flex items-center gap-2.5 rounded-2xl border border-border bg-surface px-3.5 py-[13px]">
          <span className="flex size-[34px] flex-none items-center justify-center rounded-[11px] bg-sunken text-label-alt">
            <MaterialIcon name="contact_phone" size={18} />
          </span>
          <div className="min-w-0 flex-1">
            <div className="text-[14px] font-bold text-label">연락 수단</div>
            <div className="mt-0.5 text-[12px] font-medium text-label-alt">아직 등록되지 않았어요</div>
          </div>
          <span className="flex-none rounded-[7px] bg-amber-badge px-2 py-1 text-[11px] font-bold text-amber-badge-ink">
            필요
          </span>
        </div>

        <div className="mt-2 flex gap-2.5 rounded-[14px] bg-primary-tint px-3.5 py-3">
          <MaterialIcon name="lock" size={18} className="mt-px flex-none text-accent" />
          <span className="text-[13px] leading-normal font-medium text-primary-tint-ink">
            등록한 정보는 배정된 상대에게만 공개돼요. 목록에는 보이지 않습니다.
          </span>
        </div>

        <p className="mt-[11px] text-[12px] leading-[1.55] font-medium text-label-alt">
          작성한 내용은 임시 저장해두었어요. 설정을 마치면 이어서 등록할 수 있어요.
        </p>

        <div className="mt-3.5">
          <PrimaryButton label="연락 수단 설정하러 가기 →" onClick={onSetup} />
        </div>
      </div>
    </div>
  )
}
