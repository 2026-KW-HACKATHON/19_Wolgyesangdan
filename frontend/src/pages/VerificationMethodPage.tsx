import { useState } from 'react'
import BottomActionBar from '../components/BottomActionBar'
import MaterialIcon from '../components/icons/MaterialIcon'
import NoticeBox from '../components/NoticeBox'
import PrimaryButton from '../components/PrimaryButton'
import ProgressSteps from '../components/ProgressSteps'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import type { VerificationMethod } from '../types/verification'

interface MethodOption {
  type: VerificationMethod
  title: string
  description: string
  tags: string[]
  icon: string
  /** 아이콘 타일 배경·글리프 색 Tailwind 클래스 */
  iconClassName: string
}

const OPTIONS: MethodOption[] = [
  {
    type: 'resident',
    title: '월계 주민 인증',
    description: '월계1동에 살고 있다는 걸 보여주는 서류',
    tags: ['임대차 계약서', '공과금 고지서', '등본'],
    icon: 'home_work',
    iconClassName: 'bg-primary-tint text-accent',
  },
  {
    type: 'student',
    title: '학생 인증',
    description: '광운대학교에 재학 중이라는 걸 보여주는 서류',
    tags: ['학생증', '재학증명서', '합격통지서'],
    icon: 'school',
    iconClassName: 'bg-clay-tint text-clay-ink',
  },
]

interface VerificationMethodPageProps {
  onBack: () => void
  onSkip: () => void
  onNext: (type: VerificationMethod) => void
}

/** 인증 방법 선택 (#1b). */
export default function VerificationMethodPage({
  onBack,
  onSkip,
  onNext,
}: VerificationMethodPageProps) {
  const [selected, setSelected] = useState<VerificationMethod | null>(null)

  return (
    <Screen>
      <TopBar
        title="이웃 인증"
        onBack={onBack}
        rightSlot={
          <button
            type="button"
            onClick={onSkip}
            className="cursor-pointer px-1.5 text-[13px] font-semibold text-label-alt"
          >
            나중에
          </button>
        }
      />
      <ProgressSteps total={2} current={1} />

      <div className="px-5 pt-1.5">
        <h1 className="font-hand text-[28px] leading-[1.25] font-bold text-label">
          월계1동 이웃인지 확인할게요
        </h1>
        <p className="mt-2 text-[14px] leading-[1.6] font-medium text-ink-3">
          나눔 신청은 월계1동 주민과 광운대학교 학생만 할 수 있어요. 둘 중 하나만 인증하면 돼요.
        </p>
      </div>

      <div className="flex flex-col gap-2.5 px-5 pt-5" role="radiogroup" aria-label="인증 방법">
        {OPTIONS.map((option) => {
          const isSelected = selected === option.type
          return (
            <button
              key={option.type}
              type="button"
              role="radio"
              aria-checked={isSelected}
              onClick={() => setSelected(option.type)}
              className={`flex cursor-pointer gap-[13px] rounded-[18px] bg-surface text-left ${
                isSelected
                  ? 'border-2 border-primary px-[17px] py-[15px]'
                  : 'border border-border px-[18px] py-4'
              }`}
            >
              <span
                className={`flex size-11 flex-none items-center justify-center rounded-[14px] ${option.iconClassName}`}
              >
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
                <span className="mt-1 text-[13px] leading-[1.55] font-medium text-ink-3">
                  {option.description}
                </span>
                <span className="mt-2.5 flex flex-wrap gap-[5px]">
                  {option.tags.map((tag) => (
                    <span
                      key={tag}
                      className="rounded-[7px] bg-sunken px-2 py-[3px] text-[11px] font-semibold text-ink-2"
                    >
                      {tag}
                    </span>
                  ))}
                </span>
              </span>
            </button>
          )
        })}
      </div>

      <NoticeBox
        variant="green"
        icon="lock"
        text="첨부한 서류는 인증 확인에만 쓰고, 검토가 끝나면 30일 안에 지웁니다."
      />

      <div className="flex-1" />

      <BottomActionBar>
        <PrimaryButton
          label="다음"
          disabled={!selected}
          onClick={() => selected && onNext(selected)}
        />
      </BottomActionBar>
    </Screen>
  )
}
