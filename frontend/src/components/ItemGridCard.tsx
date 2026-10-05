import Badge from './Badge'
import ItemThumb from './ItemThumb'
import { ITEM_STATUS_LABEL, itemStatusTone } from '../lib/item'
import type { ItemSummary } from '../types/item'

type ItemGridCardProps = {
  item: ItemSummary
  onClick?: () => void
}

export default function ItemGridCard({ item, onClick }: ItemGridCardProps) {
  return (
    <button type="button" onClick={onClick} className="flex flex-col gap-2 text-left">
      <ItemThumb
        imageUrl={item.thumbnailImageUrl}
        categoryGroup={item.categoryGroup}
        alt={item.name}
        className="aspect-square w-full rounded-2xl"
        iconClassName="text-[52px]"
      />
      <div className="flex flex-col gap-0.5">
        <div>
          <Badge tone={itemStatusTone(item.status)}>{ITEM_STATUS_LABEL[item.status]}</Badge>
        </div>
        <div className="truncate text-[15px] font-bold text-[var(--color-label)]">{item.name}</div>
        <div className="text-xs font-medium text-[var(--color-label-alt)]">
          {/* 세부 카테고리는 선택 입력이라 없으면 대분류를 보여준다 */}
          {item.category ?? item.categoryGroup} · {item.conditionGrade}
        </div>
        <div className="inline-flex items-center gap-1 text-xs font-bold text-[var(--color-accent)]">
          <span className="ms text-sm">autorenew</span>약 {item.estimatedCarbonReduction}kg CO₂e 절감
        </div>
      </div>
    </button>
  )
}
