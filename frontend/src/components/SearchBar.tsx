type SearchBarProps = {
  placeholder?: string
  onClick?: () => void
}

export default function SearchBar({
  placeholder = '어떤 물건을 찾고 있나요?',
  onClick,
}: SearchBarProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex h-[46px] w-full items-center gap-2 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-3.5 text-[var(--color-label-alt)]"
    >
      <span className="ms text-xl">search</span>
      <span className="text-[15px] font-medium">{placeholder}</span>
    </button>
  )
}
