import type { ReactNode } from 'react'

interface BottomActionBarProps {
  children: ReactNode
  /** true면 화면 하단에 sticky로 고정 (스크롤 콘텐츠용) */
  sticky?: boolean
}

export default function BottomActionBar({ children, sticky = true }: BottomActionBarProps) {
  return (
    <div
      className={`flex-none border-t border-border bg-surface px-5 pt-3 pb-4 ${
        sticky ? 'sticky bottom-0' : ''
      }`}
    >
      {children}
    </div>
  )
}
