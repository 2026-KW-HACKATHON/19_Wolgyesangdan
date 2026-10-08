import { formatDateTime } from '../lib/reservation'
import type { ItemDetail } from '../types/item'

/** 내 신청 상태 → 지금 단계 (1: 신청·대기, 2: 배정·재확인, 3: 받기 완료) */
const CURRENT_STEP: Record<string, number> = { WAITING: 1, SELECTED: 2, COMPLETED: 3 }

/**
 * 물품 상세 "신청하면 이렇게 진행돼요" (#189) — 신청 뒤 마감 → 자동 배정 → 24시간 안 재확인 → 받기까지를 미리 알려준다.
 * 이미 신청했으면 지금 어느 단계인지 "지금 여기"로 표시한다.
 */
export default function ApplyProcessGuide({ item }: { item: ItemDetail }) {
  const direct = item.tradeMethods.includes('DIRECT')
  const campaign = item.tradeMethods.includes('CAMPAIGN')
  const currentStep = item.myApplication ? CURRENT_STEP[item.myApplication.status] : undefined

  const steps = [
    {
      title: item.applicationDeadline
        ? `${formatDateTime(item.applicationDeadline)}까지 신청을 받아요`
        : '신청 마감까지 신청을 받아요',
      detail: '우선배정 인증(신입생·기초수급자)이 있으면 앞 순번이 돼요.',
    },
    {
      title: '마감되면 자동으로 배정돼요',
      detail: '배정되면 홈에서 알려드려요. 24시간 안에 수령 재확인을 눌러야 하고, 놓치면 다음 순번으로 넘어가요.',
    },
    direct && campaign
      ? { title: '직접 만나거나 거점에서 받아요', detail: '배정되면 등록자와 서로의 연락 수단이 공개돼요.' }
      : campaign
        ? {
            title: '거점에서 받아요',
            detail: `${item.campaign?.locationName ?? '캠페인 거점'}에서 수령 기간에 비대면으로 받아요.`,
          }
        : { title: '등록자와 연락해 직접 받아요', detail: '배정되면 서로의 연락 수단이 공개돼요.' },
  ]

  return (
    <section>
      <h3 className="mb-2 text-lg font-bold text-[var(--color-label)]">신청하면 이렇게 진행돼요</h3>
      <ol className="flex flex-col gap-3">
        {steps.map((step, i) => {
          const no = i + 1
          const here = currentStep === no
          const done = currentStep !== undefined && no < currentStep
          return (
            <li key={no} className="flex gap-3">
              <span
                className={`flex size-6 flex-none items-center justify-center rounded-full text-[12px] font-bold ${
                  here || done
                    ? 'bg-[var(--color-primary)] text-[var(--color-surface)]'
                    : 'bg-[#EBE4D1] text-[#4A4A40]'
                }`}
              >
                {no}
              </span>
              <div className="min-w-0">
                <div className="flex items-center gap-1.5 text-sm font-bold text-[var(--color-label)]">
                  {step.title}
                  {here && (
                    <span className="flex-none rounded-[6px] bg-primary-tint px-1.5 py-0.5 text-[11px] font-bold text-accent">
                      지금 여기
                    </span>
                  )}
                </div>
                <p className="mt-0.5 text-xs leading-relaxed font-medium text-[var(--color-label-alt)]">{step.detail}</p>
              </div>
            </li>
          )
        })}
      </ol>
    </section>
  )
}
