import type { ReactNode } from 'react'

type InfoBannerProps = {
  children: ReactNode
}

export default function InfoBanner({ children }: InfoBannerProps) {
  return (
    <div className="mx-5 flex items-start gap-2 rounded-2xl bg-[#E7EBD8] px-3.5 py-3">
      <span className="ms mt-0.5 flex-none text-lg text-[var(--color-accent)]">info</span>
      <p className="text-[13px] font-medium text-[#3E4A2C]">{children}</p>
    </div>
  )
}
