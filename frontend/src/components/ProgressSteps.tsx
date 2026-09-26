interface ProgressStepsProps {
  total: number
  current: number
}

/** 상단 진행 표시 — 세그먼트 바 + "n / total" 라벨. */
export default function ProgressSteps({ total, current }: ProgressStepsProps) {
  return (
    <div className="flex flex-none items-center gap-2 px-5 pb-4">
      {Array.from({ length: total }, (_, i) => (
        <span
          key={i}
          className={`h-1 flex-1 rounded-xs ${i < current ? 'bg-primary' : 'bg-track'}`}
        />
      ))}
      <span className="whitespace-nowrap text-[12px] font-bold text-label-alt">
        {current} / {total}
      </span>
    </div>
  )
}
