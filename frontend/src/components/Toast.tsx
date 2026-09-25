import type { ReactNode } from 'react'

type ToastProps = {
  visible: boolean
  children: ReactNode
}

export default function Toast({ visible, children }: ToastProps) {
  return (
    <div
      className={`pointer-events-none absolute inset-x-0 bottom-20 flex justify-center transition-opacity duration-200 ${
        visible ? 'opacity-100' : 'opacity-0'
      }`}
    >
      <div className="flex items-center gap-1.5 rounded-full bg-[var(--color-label)] px-4 py-2.5 text-[13px] font-bold text-[var(--color-screen)] shadow-[0_8px_24px_rgba(31,36,25,0.25)]">
        <span className="ms text-base">check_circle</span>
        {children}
      </div>
    </div>
  )
}
