interface PrimaryButtonProps {
  label: string
  onClick?: () => void
  disabled?: boolean
  loading?: boolean
  loadingLabel?: string
  variant?: 'primary' | 'text'
  type?: 'button' | 'submit'
  /** 바깥 여백 등 배치용 추가 클래스 */
  className?: string
}

const VARIANT_CLASS = {
  primary: 'h-13 bg-primary text-screen disabled:cursor-default disabled:bg-border-deep',
  text: 'h-[46px] bg-transparent text-accent',
}

export default function PrimaryButton({
  label,
  onClick,
  disabled,
  loading,
  loadingLabel,
  variant = 'primary',
  type = 'button',
  className = '',
}: PrimaryButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      className={`w-full cursor-pointer rounded-[14px] text-[16px] font-bold ${VARIANT_CLASS[variant]} ${className}`}
    >
      {loading ? loadingLabel ?? '처리 중…' : label}
    </button>
  )
}
