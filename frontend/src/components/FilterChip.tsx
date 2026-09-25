import type { ReactNode } from 'react'

type FilterChipProps = {
  active?: boolean
  onClick?: () => void
  children: ReactNode
}

export default function FilterChip({ active, onClick, children }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex flex-none items-center gap-1 rounded-full px-3.5 py-1.5 text-[13px] font-semibold whitespace-nowrap ${
        active
          ? 'bg-[var(--color-primary)] text-[var(--color-surface)] font-bold'
          : 'border border-[var(--color-border)] bg-[var(--color-surface)] text-[#4A4A40]'
      }`}
    >
      {children}
    </button>
  )
}
