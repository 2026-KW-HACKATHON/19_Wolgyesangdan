type PlaceholderProps = {
  title: string
}

export default function Placeholder({ title }: PlaceholderProps) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-2 px-6 text-center">
      <span className="ms text-4xl text-[var(--color-label-alt)]">construction</span>
      <p className="text-base font-bold text-[var(--color-label)]">{title} 화면 준비 중이에요</p>
    </div>
  )
}
