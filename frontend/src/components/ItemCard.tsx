import Badge from './Badge'
import { STATUS_LABEL, TRADE_METHOD_LABEL, type Item } from '../data/items'

type ItemCardProps = {
  item: Item
  onClick?: () => void
}

export default function ItemCard({ item, onClick }: ItemCardProps) {
  const statusTone = item.status === 'OPEN' ? 'primary' : item.status === 'ASSIGNING' ? 'warning' : 'neutral'

  return (
    <button type="button" onClick={onClick} className="flex w-full gap-3 py-3.5 text-left">
      <div className="flex h-[90px] w-[90px] flex-none items-center justify-center rounded-2xl bg-[#E7EBD8] text-[var(--color-primary)]">
        <span className="ms text-[34px]">{item.icon}</span>
      </div>
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex flex-wrap gap-1">
          <Badge tone={statusTone}>{STATUS_LABEL[item.status]}</Badge>
          {item.tradeMethods.map((method) => (
            <Badge key={method}>{TRADE_METHOD_LABEL[method]}</Badge>
          ))}
        </div>
        <div className="truncate text-base font-bold text-[var(--color-label)]">{item.name}</div>
        <div className="text-[13px] font-medium text-[var(--color-label-alt)]">
          {item.category} · {item.condition} · {item.deadlineLabel}
        </div>
        <div className="inline-flex items-center gap-1 text-[13px] font-bold text-[var(--color-accent)]">
          <span className="ms text-[15px]">autorenew</span>
          재사용 시 약 {item.carbonKg}kg CO₂e 절감
        </div>
      </div>
    </button>
  )
}
