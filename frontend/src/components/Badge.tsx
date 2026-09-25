import type { ReactNode } from 'react'

type BadgeTone = 'primary' | 'neutral' | 'warning'

type BadgeProps = {
  children: ReactNode
  tone?: BadgeTone
}

const TONE_CLASS: Record<BadgeTone, string> = {
  primary: 'bg-[var(--color-primary)] text-[var(--color-surface)]',
  neutral: 'bg-[#EBE4D1] text-[#4A4A40]',
  warning: 'bg-[#F7E6C9] text-[#7A4E14]',
}

export default function Badge({ children, tone = 'neutral' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-md px-1.5 py-0.5 text-[11px] font-bold whitespace-nowrap ${TONE_CLASS[tone]}`}
    >
      {children}
    </span>
  )
}
