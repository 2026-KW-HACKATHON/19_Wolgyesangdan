interface DocumentTypeChipsProps {
  label: string
  required?: boolean
  options: string[]
  value: string | null
  onChange: (value: string) => void
}

/** 서류 종류 단일 선택 칩 목록. */
export default function DocumentTypeChips({
  label,
  required,
  options,
  value,
  onChange,
}: DocumentTypeChipsProps) {
  return (
    <div className="px-5 pt-4.5">
      <div className="mb-[9px] text-[14px] font-bold text-body">
        {label}
        {required && <span className="text-terracotta"> *</span>}
      </div>
      <div className="flex flex-wrap gap-1.5" role="radiogroup" aria-label={label}>
        {options.map((option) => {
          const selected = option === value
          return (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(option)}
              className={`cursor-pointer rounded-full border px-[15px] py-2 text-[14px] ${
                selected
                  ? 'border-primary bg-primary font-bold text-screen'
                  : 'border-border bg-surface font-semibold text-ink-2'
              }`}
            >
              {option}
            </button>
          )
        })}
      </div>
    </div>
  )
}
