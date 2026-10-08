import { useState } from 'react'
import { verifyLocation } from '../api/verification'
import BottomActionBar from '../components/BottomActionBar'
import MaterialIcon from '../components/icons/MaterialIcon'
import NoticeBox from '../components/NoticeBox'
import PrimaryButton from '../components/PrimaryButton'
import ProgressSteps from '../components/ProgressSteps'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import MapPlaceholder from '../components/verification/MapPlaceholder'
import { useNeighborhoodLocation } from '../hooks/useNeighborhoodLocation'
import type { Coords, LocationStatus } from '../types/verification'

interface LocationVerificationPageProps {
  onBack: () => void
  /** "나중에" — 동네 인증 없이 홈으로 (신청·등록 불가 상태) */
  onSkip: () => void
  onVerified: () => void
}

/** GPS 동네 인증 (1b, /verify/location). 좌표를 서버로 보내 월계1동 안인지 판정한다. */
export default function LocationVerificationPage({ onBack, onSkip, onVerified }: LocationVerificationPageProps) {
  const { status, coords, dongName, locate } = useNeighborhoodLocation()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleRelocate = () => {
    setError(null)
    locate()
  }

  const handleVerify = async () => {
    if (status !== 'inside' || !coords || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      await verifyLocation(coords)
      onVerified()
    } catch (e) {
      setError(e instanceof Error ? e.message : '인증하지 못했어요. 잠시 후 다시 시도해 주세요.')
      setSubmitting(false)
    }
  }

  const denied = status === 'denied'

  return (
    <Screen>
      <TopBar
        title="동네 인증"
        onBack={onBack}
        rightSlot={
          <button type="button" onClick={onSkip} className="cursor-pointer px-1.5 text-[13px] font-semibold text-label-alt">
            나중에
          </button>
        }
      />
      <ProgressSteps total={2} current={1} />

      <div className="px-5 pt-1.5">
        <h1 className="font-hand text-[28px] leading-[1.25] font-bold text-label">월계1동 이웃인지 확인할게요</h1>
        <p className="mt-2 text-[14px] leading-[1.6] font-medium text-ink-3">
          지금 있는 위치로 월계1동인지 확인해요. 집이나 학교 근처에서 인증해 주세요.
        </p>
      </div>

      <div className="mx-5 mt-[18px] flex-none overflow-hidden rounded-[20px] border border-border bg-surface">
        {denied ? (
          <PermissionGuide />
        ) : (
          <>
            <MapPlaceholder
              dot={status === 'inside' ? 'inside' : status === 'outside' ? 'outside' : null}
              onRelocate={handleRelocate}
              relocateDisabled={status === 'locating'}
            />
            <ResultRow status={status} coords={coords} dongName={dongName} />
          </>
        )}
      </div>

      <NoticeBox
        variant="green"
        icon="lock"
        text="위치 정보는 동네 확인에만 쓰고, 정확한 위치는 다른 이웃에게 보이지 않아요."
        className="mt-3"
      />

      <div className="flex-1" />

      <BottomActionBar>
        {status === 'outside' && (
          <p role="status" className="mb-2.5 text-center text-[13px] font-semibold text-terracotta-ink">
            월계1동 안에서 다시 시도해 주세요
          </p>
        )}
        {error && (
          <p role="alert" className="mb-2.5 text-center text-[13px] font-semibold text-terracotta">
            {error}
          </p>
        )}
        {denied ? (
          <PrimaryButton label="다시 시도" onClick={handleRelocate} />
        ) : (
          <PrimaryButton
            label="이 위치로 인증하기"
            disabled={status !== 'inside'}
            loading={submitting}
            loadingLabel="인증 중…"
            onClick={handleVerify}
          />
        )}
        {!denied && (
          <RelocateButton
            emphasized={status === 'inaccurate' || status === 'unavailable'}
            disabled={status === 'locating'}
            onClick={handleRelocate}
          />
        )}
      </BottomActionBar>
    </Screen>
  )
}

const ROW_COPY: Record<Exclude<LocationStatus, 'denied'>, { title: string; caption?: string }> = {
  locating: { title: '위치를 찾는 중이에요' },
  inside: { title: '' },
  outside: { title: '', caption: '월계1동이 아니에요' },
  inaccurate: { title: '위치가 정확하지 않아요', caption: '창가나 건물 밖에서, 휴대폰으로 다시 찾아보세요' },
  unavailable: { title: '위치를 찾지 못했어요', caption: '잠시 후 다시 찾아보세요' },
}

function ResultRow({
  status,
  coords,
  dongName,
}: {
  status: Exclude<LocationStatus, 'denied'>
  coords: Coords | null
  dongName: string | null
}) {
  const accuracyText = coords ? `오차 약 ${Math.round(coords.accuracy)}m` : ''
  const copy = ROW_COPY[status]
  const warn = status === 'outside' || status === 'inaccurate' || status === 'unavailable'

  const title = status === 'inside' || status === 'outside' ? (dongName ?? '현재 위치') : copy.title
  const caption =
    status === 'inside'
      ? `현재 위치 · ${accuracyText}`
      : status === 'inaccurate'
        ? `${accuracyText} · ${copy.caption}`
        : copy.caption

  return (
    <div className="flex items-center gap-[11px] px-4 py-3.5" aria-live="polite">
      <span
        className={`flex size-[38px] flex-none items-center justify-center rounded-xl ${
          warn ? 'bg-amber-tint text-terracotta-ink' : 'bg-primary-tint text-accent'
        }`}
      >
        {status === 'locating' ? (
          <span className="size-[18px] animate-spin rounded-full border-2 border-accent/30 border-t-accent" />
        ) : (
          <MaterialIcon name={warn ? 'wrong_location' : 'location_on'} size={20} />
        )}
      </span>
      <div className="min-w-0 flex-1">
        <div className="text-[15px] font-bold text-label">{title}</div>
        {caption && <div className="mt-0.5 text-[12px] font-medium text-label-alt">{caption}</div>}
      </div>
      {status === 'inside' && (
        <span className="inline-flex flex-none items-center gap-[3px] rounded-[7px] bg-primary px-2 py-1 text-[11px] font-bold text-screen">
          <MaterialIcon name="check" size={13} />
          월계1동
        </span>
      )}
    </div>
  )
}

function PermissionGuide() {
  const [open, setOpen] = useState(false)
  return (
    <div className="flex flex-col items-center px-5 py-8 text-center">
      <span className="flex size-14 items-center justify-center rounded-full bg-amber-tint text-terracotta-ink">
        <MaterialIcon name="location_disabled" size={28} />
      </span>
      <div className="mt-3 text-[16px] font-bold text-label">위치 권한이 필요해요</div>
      <p className="mt-1 text-[13px] leading-[1.55] font-medium text-ink-3">
        월계1동인지 확인하려면 이 사이트의 위치 접근을 허용해 주세요.
      </p>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="mt-3 inline-flex cursor-pointer items-center gap-0.5 text-[13px] font-bold text-accent"
      >
        설정 방법 보기
        <MaterialIcon name={open ? 'expand_less' : 'expand_more'} size={18} />
      </button>
      {open && (
        <ol className="mt-2 w-full list-decimal rounded-xl bg-sunken py-3 pr-3 pl-7 text-left text-[12px] leading-[1.6] font-medium text-ink-3">
          <li>주소창 왼쪽의 자물쇠(또는 설정) 아이콘을 눌러요.</li>
          <li>위치 권한을 "허용"으로 바꿔요.</li>
          <li>이 화면으로 돌아와 "다시 시도"를 눌러요.</li>
        </ol>
      )}
    </div>
  )
}

interface RelocateButtonProps {
  /** 오차가 크거나 위치를 못 찾았을 때 테두리로 강조 */
  emphasized: boolean
  disabled: boolean
  onClick: () => void
}

function RelocateButton({ emphasized, disabled, onClick }: RelocateButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`mt-1 flex h-10 w-full cursor-pointer items-center justify-center gap-1 text-[13px] font-bold text-accent disabled:cursor-default disabled:opacity-50 ${
        emphasized ? 'rounded-[14px] border border-primary bg-primary-tint' : ''
      }`}
    >
      <MaterialIcon name="refresh" size={16} />
      위치 다시 찾기
    </button>
  )
}
