import { useEffect, useRef, type FormEvent } from 'react'

const BOX_CLASS =
  'flex h-[46px] w-full items-center gap-2 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-3.5 text-[var(--color-label-alt)]'

type SearchBarProps = {
  placeholder?: string
  /** 누르면 다른 화면으로 이동하는 버튼 모양 (홈). value를 넘기면 입력칸이 된다 */
  onClick?: () => void
  /** 입력칸 모드: 지금 입력 중인 검색어 */
  value?: string
  onChange?: (value: string) => void
  /** 엔터·키보드 검색 버튼 */
  onSubmit?: () => void
  /** 지우기(×) 버튼 */
  onClear?: () => void
  autoFocus?: boolean
}

export default function SearchBar({
  placeholder = '어떤 물건을 찾고 있나요?',
  onClick,
  value,
  onChange,
  onSubmit,
  onClear,
  autoFocus = false,
}: SearchBarProps) {
  const inputRef = useRef<HTMLInputElement>(null)

  // autoFocus 속성은 마운트 때만 동작해서, 홈에서 넘어온 경우처럼 나중에 켜져도 포커스되게 한다
  useEffect(() => {
    if (autoFocus) inputRef.current?.focus()
  }, [autoFocus])

  if (value === undefined) {
    return (
      <button type="button" onClick={onClick} className={BOX_CLASS}>
        <span className="ms text-xl">search</span>
        <span className="text-[15px] font-medium">{placeholder}</span>
      </button>
    )
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    // 모바일에서 검색 후 키보드를 내린다
    inputRef.current?.blur()
    onSubmit?.()
  }

  return (
    <form role="search" onSubmit={handleSubmit} className={`${BOX_CLASS} focus-within:border-[var(--color-accent)]`}>
      <span className="ms text-xl">search</span>
      <input
        ref={inputRef}
        type="search"
        enterKeyHint="search"
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        placeholder={placeholder}
        aria-label="물품 검색"
        className="min-w-0 flex-1 bg-transparent text-[15px] font-medium text-[var(--color-label)] outline-none placeholder:text-[var(--color-label-alt)] [&::-webkit-search-cancel-button]:hidden"
      />
      {value && onClear && (
        <button
          type="button"
          onClick={() => {
            onClear()
            inputRef.current?.focus()
          }}
          aria-label="검색어 지우기"
          className="flex size-6 flex-none cursor-pointer items-center justify-center rounded-full text-[var(--color-label-alt)]"
        >
          <span className="ms text-lg">cancel</span>
        </button>
      )}
    </form>
  )
}
