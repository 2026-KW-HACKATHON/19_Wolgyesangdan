import type { ReactNode } from 'react'
import MaterialIcon from './icons/MaterialIcon'

interface TopBarProps {
  title: string
  onBack?: () => void
  rightSlot?: ReactNode
}

/** 뒤로가기 + 제목 + (선택) 우측 액션으로 구성된 상단 바. */
export default function TopBar({ title, onBack, rightSlot }: TopBarProps) {
  return (
    <div className="flex h-14 flex-none items-center gap-1.5 px-3.5">
      {onBack && (
        <button
          type="button"
          onClick={onBack}
          aria-label="뒤로가기"
          className="flex cursor-pointer items-center text-label"
        >
          <MaterialIcon name="chevron_left" size={24} />
        </button>
      )}
      <div className="flex-1 text-[17px] font-bold text-label">{title}</div>
      {rightSlot}
    </div>
  )
}
