import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getItems } from '../api/items'
import SearchBar from '../components/SearchBar'
import EmptyState from '../components/EmptyState'
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

/** 주소의 ?category= 값. 없거나 모르는 값이면 '전체' */
function toCategoryFilter(value: string | null): CategoryFilter {
  return CATEGORY_FILTERS.find((c) => c === value) ?? '전체'
}
type TradeMethodFilter = TradeMethod | '전체'

/** 주소의 ?tradeMethod= 값. 없거나 모르는 값이면 '전체' */
function toTradeMethodFilter(value: string | null): TradeMethodFilter {
  return value === 'DIRECT' || value === 'CAMPAIGN' ? value : '전체'
}

/** 주소의 ?sort= 값. 없거나 모르는 값이면 최신순 */
function toSort(value: string | null): ItemSort {
  return value === 'CARBON' || value === 'CONDITION' ? value : 'LATEST'
}

// 검색·필터·정렬은 서버가 처리한다 (GET /items). 빈 검색어와 '전체'는 조건을 보내지 않는다
// 기본은 거래가 끝난 물건(배정·거래 완료)을 숨기고, "거래 끝난 물건도 보기"면 전부 (#191)
function fetchItems(
  keyword: string,
  category: CategoryFilter,
  tradeMethod: TradeMethodFilter,
  sort: ItemSort,
  showFinished: boolean,
  page: number,
) {
  return getItems({
    keyword: keyword || undefined,
    availability: showFinished ? 'ALL' : 'ACTIVE',
    categoryGroup: category === '전체' ? undefined : category,
    tradeMethod: tradeMethod === '전체' ? undefined : tradeMethod,
    sort,
    page,
    size: PAGE_SIZE,
  })
}

/** 받침 유무에 따라 '은'/'는'을 고른다. 한글이 아니면 '는' (예: '책상'은, 'TV'는) */
function topicParticle(word: string) {
  const code = word.charCodeAt(word.length - 1) - 0xac00
  return code >= 0 && code <= 11171 && code % 28 !== 0 ? '은' : '는'
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
  // 검색어·카테고리는 주소(?keyword=&category=)에 둔다 — 홈 카테고리 아이콘에서 바로 들어오고,
  // 상세에 갔다가 뒤로 와도 검색 결과·필터가 유지된다
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword')?.trim() ?? ''
  const category = toCategoryFilter(searchParams.get('category'))
  // 거래 끝난 물건(배정·거래 완료)도 볼지 — 주소(?finished=1)에 둬서 뒤로가기·새로고침에도 유지 (#191)
  const showFinished = searchParams.get('finished') === '1'
  // 입력 중인 검색어. 뒤로가기 등으로 주소의 검색어가 바뀌면 그 값으로 다시 맞춘다
  const [draft, setDraft] = useState({ keyword, text: keyword })
  const draftText = draft.keyword === keyword ? draft.text : keyword
  // 홈 검색창을 눌러 들어오면 바로 입력할 수 있게 포커스한다
  const focusSearch = (useLocation().state as { focusSearch?: boolean } | null)?.focusSearch === true
  // 거래 방식·정렬도 주소(?tradeMethod=&sort=)에 둬서 상세에 갔다 와도 유지한다. 기본값(전체·최신순)이면 주소에서 뺀다 (#195)
  const urlTradeMethod = toTradeMethodFilter(searchParams.get('tradeMethod'))
  const sortBy = toSort(searchParams.get('sort'))
  const [sortMenuOpen, setSortMenuOpen] = useState(false)
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [failed, setFailed] = useState<{ query: string; message: string } | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)

  // 거점 거래는 진행 중인 캠페인이 있을 때만 고를 수 있다 (GET /campaigns/active)
  const { active: campaignActive, loading: campaignLoading } = useActiveCampaign()
  // 진행 중인 캠페인이 없는데 주소에 거점 거래가 남아 있으면(지난 링크 등) '전체'로 본다. 확인 중에는 주소 값 그대로
  const tradeMethod: TradeMethodFilter =
    urlTradeMethod === 'CAMPAIGN' && !campaignActive && !campaignLoading ? '전체' : urlTradeMethod
  const tradeMethodFilters: TradeMethodFilter[] = campaignActive
    ? ['전체', 'DIRECT', 'CAMPAIGN']
    : ['전체', 'DIRECT']

  const query = `${keyword}|${category}|${tradeMethod}|${sortBy}|${showFinished}`

  /** 주소의 조건 하나만 바꾼다. 빈 값이면 지운다 */
  const changeParam = (key: 'keyword' | 'category' | 'finished' | 'tradeMethod' | 'sort', value: string) => {
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev)
        if (value) params.set(key, value)
        else params.delete(key)
        return params
      },
      { replace: true },
    )
  }

  const setCategory = (next: CategoryFilter) => changeParam('category', next === '전체' ? '' : next)
  const setTradeMethod = (next: TradeMethodFilter) => changeParam('tradeMethod', next === '전체' ? '' : next)
  const setSortBy = (next: ItemSort) => changeParam('sort', next === 'LATEST' ? '' : next)
  const setShowFinished = (next: boolean) => changeParam('finished', next ? '1' : '')
  // 빈 결과일 때 거래 끝난 물건을 숨기고 있으면 함께 볼 수 있게 안내한다
  const finishedAction = showFinished
    ? undefined
    : { label: '거래 끝난 물건도 보기', onClick: () => setShowFinished(true) }

  const handleSearch = () => {
    const next = draftText.trim()
    setDraft({ keyword: next, text: next })
    changeParam('keyword', next)
  }

  const handleClear = () => {
    setDraft({ keyword: '', text: '' })
    changeParam('keyword', '')
  }

  // 조건이 바뀔 때마다 첫 페이지를 다시 받는다
  useEffect(() => {
    const query = `${keyword}|${category}|${tradeMethod}|${sortBy}|${showFinished}`
    let ignore = false
    fetchItems(keyword, category, tradeMethod, sortBy, showFinished, 0)
      .then((res) => {
        if (!ignore) setLoaded({ query, items: res.content, total: res.totalElements, page: res.number, last: res.last })
      })
      .catch((e: unknown) => {
        if (!ignore) setFailed({ query, message: toMessage(e) })
      })
    return () => {
      ignore = true
    }
  }, [keyword, category, tradeMethod, sortBy, showFinished])

  // 조건을 바꾼 직후에는 이전 조건의 결과·오류를 보여주지 않는다
  const current = loaded?.query === query ? loaded : null
  const error = failed?.query === query ? failed.message : null
  const loading = !current && !error
  const items = current?.items ?? []

  const handleLoadMore = async () => {
    if (!current || current.last || loadingMore) return
    setLoadingMore(true)
    try {
      const res = await fetchItems(keyword, category, tradeMethod, sortBy, showFinished, current.page + 1)
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
        <SearchBar
          value={draftText}
          onChange={(text) => setDraft({ keyword, text })}
          onSubmit={handleSearch}
          onClear={handleClear}
          autoFocus={focusSearch}
        />
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
          role="switch"
          aria-checked={showFinished}
          onClick={() => setShowFinished(!showFinished)}
          className={`inline-flex items-center gap-0.5 text-[13px] font-semibold ${
            showFinished ? 'text-[var(--color-accent)]' : 'text-[var(--color-label-alt)]'
          }`}
        >
          <span className="ms text-base" style={{ fontVariationSettings: `'FILL' ${showFinished ? 1 : 0}` }}>
            {showFinished ? 'check_box' : 'check_box_outline_blank'}
          </span>
          거래 끝난 물건도 보기
        </button>
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
        keyword ? (
          <EmptyState
            title={`'${keyword}'${topicParticle(keyword)} 아직 장터에 없어요`}
            description={
              // 필터 때문에 안 보이는 것일 수 있으면 필터를 먼저 풀어 보게 한다
              category !== '전체' || tradeMethod !== '전체'
                ? '카테고리나 거래 방식을 전체로 바꿔 보세요'
                : '다른 이름으로 찾아보거나 나중에 다시 들러 주세요'
            }
            action={finishedAction}
          />
        ) : category !== '전체' || tradeMethod !== '전체' ? (
          <EmptyState
            title="이 조건의 물건은 아직 없어요"
            description="다른 카테고리나 거래 방식도 둘러보세요"
            action={finishedAction}
          />
        ) : (
          <EmptyState
            title="장터가 아직 조용해요"
            description="첫 물건을 내놓고 동네 장터를 열어 보세요"
            action={{ label: '물품 등록하기', onClick: () => navigate('/register') }}
          />
        )
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
