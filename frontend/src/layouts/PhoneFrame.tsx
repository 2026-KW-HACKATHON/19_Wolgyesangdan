import type { ReactNode } from 'react'

type PhoneFrameProps = {
  children: ReactNode
}

export default function PhoneFrame({ children }: PhoneFrameProps) {
  return (
    <div className="flex min-h-dvh justify-center bg-[var(--color-bg)] sm:items-center sm:py-8">
      <div className="flex h-dvh w-full max-w-md flex-col overflow-hidden border-[var(--color-border)] bg-[var(--color-screen)] sm:h-[calc(100dvh-4rem)] sm:rounded-[28px] sm:border sm:shadow-[0_18px_40px_rgba(46,41,25,0.12)]">
        {children}
      </div>
    </div>
  )
}
