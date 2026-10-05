import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getItems } from '../api/items'
import SearchBar from '../components/SearchBar'
import FilterChip from '../components/FilterChip'
import ItemGridCard from '../components/ItemGridCard'
import { TRADE_METHOD_LABEL } from '../lib/item'
import type { CategoryGroup, ItemSort, ItemSummary, TradeMethod } from '../types/item'
import { useActiveCampaign } from '../hooks/useActiveCampaign'

const CATEGORY_FILTERS: (CategoryGroup | '전체')[] = ['전체', '가구', '가전', '주방', '생활', '기타']

const SORT_LABEL: Record<ItemSort, string> = {
  LATEST: '최신순',
  CARBON: '탄소 절감순',
  CONDITION: '상태순',
}

const PAGE_SIZE = 20

type CategoryFilter = CategoryGroup | '전체'
type TradeMethodFilter = TradeMethod | '전체'

// 필터·정렬은 서버가 처리한다 (GET /items). '전체'는 조건을 보내지 않는다
function fetchItems(category: CategoryFilter, tradeMethod: TradeMethodFilter, sort: ItemSort, page: number) {
  return getItems({
    categoryGroup: category === '전체' ? undefined : category,
    tradeMethod: tradeMethod === '전체' ? undefined : tradeMethod,
    sort,
    page,
    size: PAGE_SIZE,
  })
}

function toMessage(e: unknown) {
  return e instanceof ApiError ? e.message : '물품을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
}

/** 한 검색 조건으로 지금까지 받아온 결과 */
interface Loaded {
  /** 어떤 조건의 결과인지 — 조건이 바뀌면 이전 결과를 버린다 */
  query: string
  items: ItemSummary[]
  total: number
  /** 마지막으로 받은 페이지 (0부터) */
  page: number
  last: boolean
}

export default function ItemList() {
  const navigate = useNavigate()
  const [category, setCategory] = useState<CategoryFilter>('전체')
  const [tradeMethod, setTradeMethod] = useState<TradeMethodFilter>('전체')
  const [sortBy, setSortBy] = useState<ItemSort>('LATEST')
  const [sortMenuOpen, setSortMenuOpen] = useState(false)
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [failed, setFailed] = useState<{ query: string; message: string } | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)

  // 거점 거래는 진행 중인 캠페인이 있을 때만 고를 수 있다 (GET /campaigns/active)
  const { active: campaignActive } = useActiveCampaign()
  const tradeMethodFilters: TradeMethodFilter[] = campaignActive
    ? ['전체', 'DIRECT', 'CAMPAIGN']
    : ['전체', 'DIRECT']

  const query = `${category}|${tradeMethod}|${sortBy}`

  // 조건이 바뀔 때마다 첫 페이지를 다시 받는다
  useEffect(() => {
    const query = `${category}|${tradeMethod}|${sortBy}`
    let ignore = false
    fetchItems(category, tradeMethod, sortBy, 0)
      .then((res) => {
        if (!ignore) setLoaded({ query, items: res.content, total: res.totalElements, page: res.number, last: res.last })
      })
      .catch((e: unknown) => {
        if (!ignore) setFailed({ query, message: toMessage(e) })
      })
    return () => {
      ignore = true
    }
  }, [category, tradeMethod, sortBy])

  // 조건을 바꾼 직후에는 이전 조건의 결과·오류를 보여주지 않는다
  const current = loaded?.query === query ? loaded : null
  const error = failed?.query === query ? failed.message : null
  const loading = !current && !error
  const items = current?.items ?? []

  const handleLoadMore = async () => {
    if (!current || current.last || loadingMore) return
    setLoadingMore(true)
    try {
      const res = await fetchItems(category, tradeMethod, sortBy, current.page + 1)
      // 받는 사이 조건이 바뀌었으면 버린다
      setLoaded((prev) =>
        prev && prev.query === current.query
          ? { ...prev, items: [...prev.items, ...res.content], total: res.totalElements, page: res.number, last: res.last }
          : prev,
      )
    } catch (e) {
      setFailed({ query: current.query, message: toMessage(e) })
    } finally {
      setLoadingMore(false)
    }
  }

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
            {t === '전체' ? t : TRADE_METHOD_LABEL[t]}
          </FilterChip>
        ))}
      </div>

      <div className="h-px bg-[var(--color-border)]" />

      <div className="relative flex items-center gap-2 px-5 py-1">
        <span className="flex-1 text-[13px] font-medium text-[var(--color-label-alt)]">
          {current ? `${current.total}개의 물품` : ''}
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
            {(Object.keys(SORT_LABEL) as ItemSort[]).map((key) => (
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

      {loading && (
        <p className="px-5 py-8 text-center text-sm font-medium text-[var(--color-label-alt)]">
          물품을 불러오는 중이에요…
        </p>
      )}

      {error && (
        <p role="alert" className="px-5 py-4 text-center text-sm font-semibold text-terracotta">
          {error}
        </p>
      )}

      {current && items.length === 0 && (
        <p className="px-5 py-8 text-center text-sm font-medium text-[var(--color-label-alt)]">
          조건에 맞는 물품이 없어요
        </p>
      )}

      {current && !current.last && (
        <button
          type="button"
          onClick={handleLoadMore}
          disabled={loadingMore}
          className="mx-5 mt-2 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] py-2.5 text-[13px] font-bold text-[#4A4A40] disabled:opacity-60"
        >
          {loadingMore ? '불러오는 중…' : '더 보기'}
        </button>
      )}
    </div>
  )
}
