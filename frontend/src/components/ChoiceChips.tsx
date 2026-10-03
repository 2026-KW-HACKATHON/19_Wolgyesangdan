import { chipClassName } from './chipStyles'

interface ChoiceChipsProps<T extends string> {
  label: string
  required?: boolean
  options: readonly T[]
  value: T | null
  onChange: (value: T) => void
}

/** 단일 선택 칩 목록 (서류 종류, 카테고리, 상태 등). */
export default function ChoiceChips<T extends string>({
  label,
  required,
  options,
  value,
  onChange,
}: ChoiceChipsProps<T>) {
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
              className={chipClassName(selected)}
            >
              {option}
            </button>
          )
        })}
      </div>
    </div>
  )
}
