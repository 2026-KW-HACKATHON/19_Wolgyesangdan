import HourglassIcon from '../components/HourglassIcon'
import MaterialIcon from '../components/icons/MaterialIcon'
import PrimaryButton from '../components/PrimaryButton'
import Screen from '../components/Screen'
import { PRIORITY_OPTIONS } from '../data/priorityVerification'
import type { PrioritySubmitMeta } from '../types/verification'

interface VerificationDonePageProps {
  meta: PrioritySubmitMeta
  onGoHome: () => void
  onViewStatus: () => void
}

function formatMonthDay(date: Date) {
  return `${date.getMonth() + 1}.${date.getDate()}`
}

/** 인증 신청 접수 완료 (1d·1e 공통, /verify/done). 동네 인증은 끝났고 우선배정은 검토 중인 상태. */
export default function VerificationDonePage({ meta, onGoHome, onViewStatus }: VerificationDonePageProps) {
  const option = PRIORITY_OPTIONS[meta.type]

  return (
    <Screen>
      <div className="flex flex-1 flex-col items-center px-5 pt-[22px] pb-6 text-center">
        <span className="flex size-16 items-center justify-center rounded-full bg-primary-tint text-accent">
          <HourglassIcon size={32} />
        </span>
        <h1 className="mt-3 font-hand text-[26px] leading-[1.25] font-bold text-label">인증 신청이 접수됐어요</h1>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">
          동네 인증은 끝났으니 지금 바로 나눔을 신청할 수 있어요. 우선배정은 검토가 끝나면 적용돼요.
        </p>

        <div className="mt-4 w-full overflow-hidden rounded-2xl border border-border bg-surface text-left">
          <SummaryRow
            icon="location_on"
            tileClassName="bg-primary-tint text-accent"
            title="동네 인증 · 월계1동"
            caption="GPS 인증"
            badge="완료"
            badgeClassName="bg-primary text-screen"
          />
          <div className="mx-4 h-px bg-border" />
          <SummaryRow
            icon={option.icon}
            tileClassName={option.tileClassName}
            title={option.title}
            caption={`${formatMonthDay(meta.submittedAt)} 신청 · 서류 ${meta.docCount}장`}
            badge="검토 중"
            badgeClassName="bg-amber-badge text-amber-badge-ink"
          />
        </div>

        <PrimaryButton className="mt-3.5" label="홈으로" onClick={onGoHome} />
        <button
          type="button"
          onClick={onViewStatus}
          className="mt-1.5 h-[46px] w-full cursor-pointer text-[14px] font-bold text-accent"
        >
          마이페이지에서 상태 보기
        </button>
      </div>
    </Screen>
  )
}

interface SummaryRowProps {
  icon: string
  tileClassName: string
  title: string
  caption: string
  badge: string
  /** 배지 배경·글자색 클래스 */
  badgeClassName: string
}

function SummaryRow({ icon, tileClassName, title, caption, badge, badgeClassName }: SummaryRowProps) {
  return (
    <div className="flex items-center gap-2.5 px-4 py-[13px]">
      <span className={`flex size-9 flex-none items-center justify-center rounded-xl ${tileClassName}`}>
        <MaterialIcon name={icon} size={19} />
      </span>
      <div className="min-w-0 flex-1">
        <div className="text-[14px] font-bold text-label">{title}</div>
        <div className="mt-0.5 text-[12px] font-medium text-label-alt">{caption}</div>
      </div>
      <span className={`flex-none rounded-[7px] px-2 py-1 text-[11px] font-bold ${badgeClassName}`}>{badge}</span>
    </div>
  )
}
