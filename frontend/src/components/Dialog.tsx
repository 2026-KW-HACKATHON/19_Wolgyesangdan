import { useEffect, useId, useRef, type ReactNode } from 'react'
import MaterialIcon from './icons/MaterialIcon'

export type DialogTone = 'primary' | 'danger'

const ICON_TONE: Record<DialogTone, string> = {
  primary: 'bg-primary-tint text-accent',
  danger: 'bg-amber-tint text-terracotta',
}

interface DialogProps {
  /** Material Symbols 아이콘 이름 */
  icon: string
  tone?: DialogTone
  title: string
  description: string
  /** 아래 버튼들 */
  children: ReactNode
  /** 바깥(어두운 배경) 클릭·Esc로 닫을 때 */
  onClose: () => void
}

/**
 * 서비스 공용 팝업 틀 (#192) — 브라우저 기본 팝업(window.confirm·alert) 대신 이것만 쓴다.
 * 둥근 카드 · 아이콘 · 손글씨체 제목 · 설명 · 버튼, 어두운 배경. 바깥 클릭·Esc로 닫히고 열리면 팝업에 포커스한다.
 */
export default function Dialog({ icon, tone = 'primary', title, description, children, onClose }: DialogProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  const titleId = useId()

  useEffect(() => {
    dialogRef.current?.focus()
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-label/50 px-8" onClick={onClose}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[320px] rounded-3xl bg-screen px-6 pt-6 pb-4 text-center shadow-[0_12px_32px_rgba(31,36,25,0.22)] outline-none"
      >
        <span className={`mx-auto flex size-13 items-center justify-center rounded-2xl ${ICON_TONE[tone]}`}>
          <MaterialIcon name={icon} size={26} />
        </span>
        <h2 id={titleId} className="mt-3 font-hand text-[26px] leading-[1.25] font-bold text-label">
          {title}
        </h2>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">{description}</p>
        {children}
      </div>
    </div>
  )
}
