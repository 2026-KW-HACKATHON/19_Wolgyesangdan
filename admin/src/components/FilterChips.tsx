interface FilterChipsProps<T extends string> {
  options: { value: T; label: string }[]
  value: T
  onChange: (value: T) => void
  /** 스크린리더용 그룹 이름 (예: "상태") */
  label: string
}

/** 목록 위 필터 칩 (예: 검토 대기 · 승인 · 반려 · 전체). 하나만 고른다 */
export default function FilterChips<T extends string>({ options, value, onChange, label }: FilterChipsProps<T>) {
  return (
    <div role="radiogroup" aria-label={label} className="flex flex-wrap gap-1.5">
      {options.map((option) => {
        const selected = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={`h-8 rounded-full px-3.5 text-[13px] font-bold whitespace-nowrap ${
              selected ? 'bg-primary text-surface' : 'border border-border bg-surface text-body'
            }`}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
