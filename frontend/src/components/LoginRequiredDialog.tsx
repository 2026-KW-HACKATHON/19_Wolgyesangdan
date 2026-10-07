import { useEffect, useRef } from 'react'
import MaterialIcon from './icons/MaterialIcon'
import PrimaryButton from './PrimaryButton'

interface LoginRequiredDialogProps {
  /** 무엇을 하려면 로그인이 필요한지 */
  description: string
  onLogin: () => void
  onCancel: () => void
}

/** 로그인이 필요한 기능을 누르면 바로 로그인 화면으로 넘기지 않고 먼저 띄우는 팝업 (#173) */
export default function LoginRequiredDialog({ description, onLogin, onCancel }: LoginRequiredDialogProps) {
  const dialogRef = useRef<HTMLDivElement>(null)

  // 열리면 팝업에 포커스, Esc로 닫는다
  useEffect(() => {
    dialogRef.current?.focus()
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onCancel])

  return (
    // 바깥(어두운 배경)을 누르면 닫는다
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-label/50 px-8" onClick={onCancel}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-required-title"
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[320px] rounded-3xl bg-screen px-6 pt-6 pb-4 text-center shadow-[0_12px_32px_rgba(31,36,25,0.22)] outline-none"
      >
        <span className="mx-auto flex size-13 items-center justify-center rounded-2xl bg-primary-tint text-accent">
          <MaterialIcon name="lock" size={26} />
        </span>
        <h2 id="login-required-title" className="mt-3 font-hand text-[26px] leading-[1.25] font-bold text-label">
          로그인이 필요해요
        </h2>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">{description}</p>
        <PrimaryButton className="mt-5" label="로그인하러 가기" onClick={onLogin} />
        <PrimaryButton className="mt-1" variant="text" label="나중에 할게요" onClick={onCancel} />
      </div>
    </div>
  )
}
