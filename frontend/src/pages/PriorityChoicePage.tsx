import { useState } from 'react'
import BottomActionBar from '../components/BottomActionBar'
import MaterialIcon from '../components/icons/MaterialIcon'
import PrimaryButton from '../components/PrimaryButton'
import ProgressSteps from '../components/ProgressSteps'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import { PRIORITY_OPTIONS, PRIORITY_TYPES } from '../data/priorityVerification'
import type { PriorityType } from '../types/verification'

interface PriorityChoicePageProps {
  onBack: () => void
  /** "건너뛰기" / "괜찮아요, 건너뛸게요" */
  onSkip: () => void
  onNext: (type: PriorityType) => void
}

/**
 * 우선배정 인증 선택 (1c, /verify/priority). 선택 사항이라 건너뛰어도 나눔 신청은 그대로 할 수 있다.
 * 단일 선택 — 둘 다 해당하면 하나를 먼저 하고 마이페이지에서 나머지를 추가한다.
 */
export default function PriorityChoicePage({ onBack, onSkip, onNext }: PriorityChoicePageProps) {
  const [selected, setSelected] = useState<PriorityType | null>(null)

  return (
    <Screen>
      <TopBar
        title="우선배정 인증"
        onBack={onBack}
        rightSlot={
          <button type="button" onClick={onSkip} className="cursor-pointer px-1.5 text-[13px] font-semibold text-label-alt">
            건너뛰기
          </button>
        }
      />
      <ProgressSteps total={2} current={2} />

      <div className="px-5 pt-1.5">
        <span className="inline-flex items-center gap-1 rounded-lg bg-primary-tint px-[9px] py-1 text-[12px] font-bold text-primary-dark">
          <MaterialIcon name="check_circle" size={14} />
          동네 인증 완료
        </span>
        <div className="mt-3.5">
          <span className="rounded-[7px] bg-sunken px-2 py-[3px] text-[12px] font-bold text-ink-2">선택 사항</span>
        </div>
        <h1 className="mt-2 font-hand text-[28px] leading-[1.25] font-bold text-label">
          해당하면 우선배정을
          <br />
          받을 수 있어요
        </h1>
        <p className="mt-2 text-[14px] leading-[1.6] font-medium text-ink-3">
          인증하면 대기열에서 가산점을 받아요. 하지 않아도 나눔 신청은 그대로 할 수 있어요.
        </p>
      </div>

      <div className="flex flex-col gap-2.5 px-5 pt-[18px]" role="radiogroup" aria-label="우선배정 인증 유형">
        {PRIORITY_TYPES.map((type) => {
          const option = PRIORITY_OPTIONS[type]
          const isSelected = selected === type
          return (
            <button
              key={type}
              type="button"
              role="radio"
              aria-checked={isSelected}
              onClick={() => setSelected(type)}
              className={`flex cursor-pointer gap-[13px] rounded-[18px] bg-surface text-left ${
                isSelected ? 'border-2 border-primary px-[17px] py-[15px]' : 'border border-border px-[18px] py-4'
              }`}
            >
              <span className={`flex size-11 flex-none items-center justify-center rounded-[14px] ${option.tileClassName}`}>
                <MaterialIcon name={option.icon} size={22} />
              </span>
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="flex items-center gap-1.5">
                  <span className="text-[16px] font-bold text-label">{option.title}</span>
                  <MaterialIcon
                    name={isSelected ? 'check_circle' : 'radio_button_unchecked'}
                    size={20}
                    className={`ml-auto ${isSelected ? 'text-accent' : 'text-ink-disabled'}`}
                  />
                </span>
                <span className="mt-1 text-[13px] leading-[1.55] font-medium text-ink-3">{option.description}</span>
                <span className="mt-[9px] flex flex-wrap gap-[5px]">
                  {option.docTypes.map((doc) => (
                    <span key={doc} className="rounded-[7px] bg-sunken px-2 py-[3px] text-[11px] font-semibold text-ink-2">
                      {doc}
                    </span>
                  ))}
                </span>
              </span>
            </button>
          )
        })}
      </div>

      <p className="px-5 pt-3 text-[12px] leading-[1.55] font-medium text-label-alt">나중에 마이페이지에서도 인증할 수 있어요.</p>

      <div className="flex-1" />

      <BottomActionBar>
        <PrimaryButton label="다음" disabled={!selected} onClick={() => selected && onNext(selected)} />
        <button
          type="button"
          onClick={onSkip}
          className="mt-1 flex h-10 w-full cursor-pointer items-center justify-center text-[14px] font-bold text-label-alt"
        >
          괜찮아요, 건너뛸게요
        </button>
      </BottomActionBar>
    </Screen>
  )
}
