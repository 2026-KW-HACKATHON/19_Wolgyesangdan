import MaterialIcon from '../components/icons/MaterialIcon'
import PrimaryButton from '../components/PrimaryButton'
import Screen from '../components/Screen'
import type { VerificationSubmitMeta } from '../types/verification'

interface VerificationCompletePageProps {
  meta: VerificationSubmitMeta
  onGoHome: () => void
  onViewStatus: () => void
}

function formatSubmittedDate(date: Date) {
  return `${date.getMonth() + 1}.${date.getDate()}`
}

/** 인증 신청 접수 완료 (#1d, 접수 완료 부분). 주민/학생 인증 공통 화면. */
export default function VerificationCompletePage({
  meta,
  onGoHome,
  onViewStatus,
}: VerificationCompletePageProps) {
  const icon = meta.type === 'student' ? 'school' : 'home_work'

  return (
    <Screen>
      <div className="flex flex-1 flex-col items-center px-5 pt-[22px] pb-6 text-center">
        <span className="flex size-16 items-center justify-center rounded-full bg-primary-tint text-accent">
          <MaterialIcon name="hourglass_top" size={32} />
        </span>
        <h1 className="mt-3 font-hand text-[26px] leading-[1.25] font-bold text-label">
          인증 신청이 접수됐어요
        </h1>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">
          검토는 보통 1~2일 걸려요. 그동안에도 물품은 자유롭게 둘러볼 수 있어요.
        </p>

        <div className="mt-4 flex w-full items-center gap-2.5 rounded-2xl border border-border bg-surface px-4 py-3.5 text-left">
          <span className="flex size-9 flex-none items-center justify-center rounded-xl bg-sunken text-ink-2">
            <MaterialIcon name={icon} size={19} />
          </span>
          <div className="min-w-0 flex-1">
            <div className="text-[14px] font-bold text-label">{meta.label}</div>
            <div className="mt-0.5 text-[12px] font-medium text-label-alt">
              {formatSubmittedDate(meta.submittedAt)} 신청 · 서류 {meta.docCount}장
            </div>
          </div>
          <span className="flex-none rounded-[7px] bg-amber-badge px-2 py-1 text-[11px] font-bold text-amber-badge-ink">
            검토 중
          </span>
        </div>

        <PrimaryButton className="mt-3.5" label="홈으로" onClick={onGoHome} />
        <PrimaryButton
          className="mt-1.5"
          variant="text"
          label="마이페이지에서 상태 보기"
          onClick={onViewStatus}
        />
      </div>
    </Screen>
  )
}
