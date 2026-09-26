import Badge from './Badge'
import { STATUS_LABEL, type Item } from '../data/items'

type ItemGridCardProps = {
  item: Item
  onClick?: () => void
}

export default function ItemGridCard({ item, onClick }: ItemGridCardProps) {
  const statusTone = item.status === 'OPEN' ? 'primary' : item.status === 'ASSIGNING' ? 'warning' : 'neutral'

  return (
    <button type="button" onClick={onClick} className="flex flex-col gap-2 text-left">
      <div className="flex aspect-square items-center justify-center rounded-2xl bg-[#E7EBD8] text-[var(--color-primary)]">
        <span className="ms text-[52px]">{item.icon}</span>
      </div>
      <div className="flex flex-col gap-0.5">
        <div>
          <Badge tone={statusTone}>{STATUS_LABEL[item.status]}</Badge>
        </div>
        <div className="truncate text-[15px] font-bold text-[var(--color-label)]">{item.name}</div>
        <div className="text-xs font-medium text-[var(--color-label-alt)]">
          {item.category} · {item.condition}
        </div>
        <div className="inline-flex items-center gap-1 text-xs font-bold text-[var(--color-accent)]">
          <span className="ms text-sm">autorenew</span>약 {item.carbonKg}kg CO₂e 절감
        </div>
      </div>
    </button>
  )
}
