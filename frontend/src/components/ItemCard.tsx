import Badge from './Badge'
import ItemThumb from './ItemThumb'
import { ITEM_STATUS_LABEL, TRADE_METHOD_LABEL, formatDeadline, itemStatusTone } from '../lib/item'
import type { ItemSummary } from '../types/item'

type ItemCardProps = {
  item: ItemSummary
  onClick?: () => void
}

export default function ItemCard({ item, onClick }: ItemCardProps) {
  // 세부 카테고리는 선택 입력이라 없으면 대분류를 보여준다
  const meta = [item.category ?? item.categoryGroup, item.conditionGrade, formatDeadline(item.applicationDeadline)]
    .filter(Boolean)
    .join(' · ')

  return (
    <button type="button" onClick={onClick} className="flex w-full gap-3 py-3.5 text-left">
      <ItemThumb
        imageUrl={item.thumbnailImageUrl}
        categoryGroup={item.categoryGroup}
        alt={item.name}
        className="h-[90px] w-[90px] flex-none rounded-2xl"
        iconClassName="text-[34px]"
      />
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex flex-wrap gap-1">
          <Badge tone={itemStatusTone(item.status)}>{ITEM_STATUS_LABEL[item.status]}</Badge>
          {item.tradeMethods.map((method) => (
            <Badge key={method}>{TRADE_METHOD_LABEL[method]}</Badge>
          ))}
        </div>
        <div className="truncate text-base font-bold text-[var(--color-label)]">{item.name}</div>
        <div className="text-[13px] font-medium text-[var(--color-label-alt)]">{meta}</div>
        <div className="inline-flex items-center gap-1 text-[13px] font-bold text-[var(--color-accent)]">
          <span className="ms text-[15px]">autorenew</span>
          재사용 시 약 {item.estimatedCarbonReduction}kg CO₂e 절감
        </div>
      </div>
    </button>
  )
}
