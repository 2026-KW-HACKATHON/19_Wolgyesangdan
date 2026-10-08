import type { ReactNode } from 'react'

/**
 * 상태 배지 색. 시안 기준
 * - waiting: 검토 대기 · 답변 대기 · 배정 중
 * - done: 승인 · 답변 완료 · 신청 가능
 * - rejected: 반려
 * - muted: 숨김
 */
export type StatusTone = 'waiting' | 'done' | 'rejected' | 'muted'

const TONE_CLASS: Record<StatusTone, string> = {
  waiting: 'bg-amber-badge text-amber-badge-ink',
  done: 'bg-primary-tint text-primary-dark',
  rejected: 'bg-amber-tint text-terracotta-ink',
  muted: 'bg-sunken text-ink-2',
}

export default function StatusBadge({ tone, children }: { tone: StatusTone; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center rounded-[7px] px-2 py-[3px] text-[11px] font-bold whitespace-nowrap ${TONE_CLASS[tone]}`}
    >
      {children}
    </span>
  )
}
