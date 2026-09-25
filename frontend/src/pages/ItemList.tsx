import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import SearchBar from '../components/SearchBar'
import FilterChip from '../components/FilterChip'
import ItemGridCard from '../components/ItemGridCard'
import { CONDITION_RANK, ITEMS, type CategoryGroup, type TradeMethod } from '../data/items'

const CATEGORY_FILTERS: (CategoryGroup | '전체')[] = ['전체', '가구', '가전', '주방', '생활', '기타']

type SortKey = 'latest' | 'carbon' | 'condition'

const SORT_LABEL: Record<SortKey, string> = {
  latest: '최신순',
  carbon: '탄소 절감순',
  condition: '상태순',
}

// TODO: 실제 캠페인 활성 여부는 백엔드 연결 후 API 응답으로 대체
const CAMPAIGN_ACTIVE = true

export default function ItemList() {
  const navigate = useNavigate()
  const [category, setCategory] = useState<CategoryGroup | '전체'>('전체')
  const [tradeMethod, setTradeMethod] = useState<TradeMethod | '전체'>('전체')
  const [sortBy, setSortBy] = useState<SortKey>('latest')
  const [sortMenuOpen, setSortMenuOpen] = useState(false)

  const tradeMethodFilters: (TradeMethod | '전체')[] = CAMPAIGN_ACTIVE
    ? ['전체', 'DIRECT', 'CAMPAIGN']
    : ['전체', 'DIRECT']

  const items = useMemo(() => {
    const filtered = ITEMS.filter((item) => {
      const matchesCategory = category === '전체' || item.categoryGroup === category
      const matchesTradeMethod = tradeMethod === '전체' || item.tradeMethods.includes(tradeMethod)
      return matchesCategory && matchesTradeMethod
    })

    return [...filtered].sort((a, b) => {
      if (sortBy === 'carbon') return b.carbonKg - a.carbonKg
      if (sortBy === 'condition') return CONDITION_RANK[a.condition] - CONDITION_RANK[b.condition]
      return Number(b.id) - Number(a.id)
    })
  }, [category, tradeMethod, sortBy])

  return (
    <div className="flex flex-col gap-2.5 pt-3.5 pb-6">
      <div className="px-5">
        <SearchBar />
      </div>

      <div className="flex gap-1.5 overflow-x-auto px-5 [scrollbar-width:none]">
        {CATEGORY_FILTERS.map((c) => (
          <FilterChip key={c} active={category === c} onClick={() => setCategory(c)}>
            {c}
          </FilterChip>
        ))}
      </div>

      <div className="flex gap-1.5 overflow-x-auto px-5 [scrollbar-width:none]">
        {tradeMethodFilters.map((t) => (
          <FilterChip key={t} active={tradeMethod === t} onClick={() => setTradeMethod(t)}>
            {t === 'DIRECT' ? '직거래' : t === 'CAMPAIGN' ? '거점 수령' : t}
          </FilterChip>
        ))}
      </div>

      <div className="h-px bg-[var(--color-border)]" />

      <div className="relative flex items-center gap-2 px-5 py-1">
        <span className="flex-1 text-[13px] font-medium text-[var(--color-label-alt)]">
          {items.length}개의 물품
        </span>
        <button
          type="button"
          onClick={() => setSortMenuOpen((open) => !open)}
          className="inline-flex items-center gap-0.5 text-[13px] font-bold text-[#4A4A40]"
        >
          {SORT_LABEL[sortBy]}
          <span className="ms text-base">{sortMenuOpen ? 'expand_less' : 'expand_more'}</span>
        </button>

        {sortMenuOpen && (
          <div className="absolute top-full right-5 z-10 flex flex-col gap-1 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-2 shadow-[0_8px_24px_rgba(46,41,25,0.12)]">
            {(Object.keys(SORT_LABEL) as SortKey[]).map((key) => (
              <button
                key={key}
                type="button"
                onClick={() => {
                  setSortBy(key)
                  setSortMenuOpen(false)
                }}
                className={`rounded-xl px-3 py-1.5 text-left text-[13px] whitespace-nowrap ${
                  sortBy === key
                    ? 'font-bold text-[var(--color-accent)]'
                    : 'font-medium text-[var(--color-label-alt)]'
                }`}
              >
                {SORT_LABEL[key]}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="grid grid-cols-2 gap-x-3 gap-y-5 px-5 pt-1">
        {items.map((item) => (
          <ItemGridCard key={item.id} item={item} onClick={() => navigate(`/items/${item.id}`)} />
        ))}
      </div>

      {items.length === 0 && (
        <p className="px-5 py-8 text-center text-sm font-medium text-[var(--color-label-alt)]">
          조건에 맞는 물품이 없어요
        </p>
      )}
    </div>
  )
}
