import type { ReactNode } from 'react'

interface TextFieldProps {
  label: string
  required?: boolean
  value: string
  onChange: (value: string) => void
  placeholder?: string
  readOnly?: boolean
  rightSlot?: ReactNode
  inputMode?: 'text' | 'numeric'
}

/** 48px 높이의 단일 라인 입력 필드. */
export default function TextField({
  label,
  required,
  value,
  onChange,
  placeholder,
  readOnly,
  rightSlot,
  inputMode = 'text',
}: TextFieldProps) {
  return (
    <div className="flex-1">
      <label className="mb-2 block text-[14px] font-bold text-body">
        {label}
        {required && <span className="text-terracotta"> *</span>}
      </label>
      <div className="flex h-12 items-center gap-2 rounded-[14px] border border-border bg-surface px-3.5">
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          readOnly={readOnly}
          inputMode={inputMode}
          className="min-w-0 flex-1 bg-transparent p-0 text-[15px] font-semibold text-label outline-none placeholder:font-medium placeholder:text-label-alt"
        />
        {rightSlot}
      </div>
    </div>
  )
}
