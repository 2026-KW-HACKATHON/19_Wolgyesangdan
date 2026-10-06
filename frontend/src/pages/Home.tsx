import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getItems } from '../api/items'
import CampaignBanner from '../components/CampaignBanner'
import EmptyState from '../components/EmptyState'
import SearchBar from '../components/SearchBar'
import ItemCard from '../components/ItemCard'
import InfoBanner from '../components/InfoBanner'
import type { ItemSummary } from '../types/item'
import { useActiveCampaign } from '../hooks/useActiveCampaign'
import { isLoggedIn } from '../lib/authStorage'

const CATEGORIES = [
  { key: '가구', icon: 'chair' },
  { key: '가전', icon: 'bolt' },
  { key: '주방', icon: 'local_cafe' },
  { key: '생활', icon: 'home' },
  { key: '기타', icon: 'more_horiz' },
]

const PREVIEW_COUNT = 4

export default function Home() {
  const navigate = useNavigate()
  const loggedIn = isLoggedIn()
  // 캠페인 배너 (GET /campaigns/active). 받는 중이거나 실패하면 배너 자리를 비워둔다
  const { campaign, active: campaignActive } = useActiveCampaign()
  // 최신 물품 4개 (GET /items). null은 아직 받는 중
  const [previewItems, setPreviewItems] = useState<ItemSummary[] | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let ignore = false
    getItems({ size: PREVIEW_COUNT })
      .then((res) => {
        if (!ignore) setPreviewItems(res.content)
      })
      .catch(() => {
        if (!ignore) setFailed(true)
      })
    return () => {
      ignore = true
    }
  }, [])

  return (
    <div className="flex flex-col gap-5 pb-6">
      <header className="flex items-center gap-2 px-5 pt-3.5">
        <span className="text-2xl font-bold text-[var(--color-accent)]">월계상단</span>
        <span className="rounded-md bg-[#EBE4D1] px-2 py-0.5 text-xs font-semibold text-[var(--color-label-alt)]">
          월계1동
        </span>
        <span className="flex-1" />
        {!loggedIn && (
          <button
            type="button"
            onClick={() => navigate('/login')}
            className="text-[13px] font-bold text-[var(--color-accent)]"
          >
            로그인
          </button>
        )}
      </header>

      {campaign !== undefined && (
        // 캠페인 상세 화면은 따로 없어서, 캠페인 기간·거래 현황을 보여주는 탄소 리포트 "이번 캠페인" 탭으로 보낸다
        <CampaignBanner campaign={campaign} onDetail={() => navigate('/carbon-report?scope=CAMPAIGN')} />
      )}

      <div className="px-5">
        <SearchBar onClick={() => navigate('/browse', { state: { focusSearch: true } })} />
      </div>

      <div className="grid grid-cols-5 px-3">
        {CATEGORIES.map((c) => (
          <button
            key={c.key}
            type="button"
            onClick={() => navigate(`/browse?category=${encodeURIComponent(c.key)}`)}
            className="flex flex-col items-center gap-1.5 py-2"
          >
            <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#E7EBD8] text-[var(--color-primary)]">
              <span className="ms text-[23px]">{c.icon}</span>
            </span>
            <span className="text-[13px] font-semibold text-[#4A4A40]">{c.key}</span>
          </button>
        ))}
      </div>

      <div>
        <div className="flex items-end gap-2 px-5 pb-2">
          <div className="min-w-0 flex-1">
            <h2 className="text-[23px] font-bold text-[var(--color-label)]">
              지금 새로운 주인을 기다려요
            </h2>
            <p className="mt-0.5 text-[13px] font-medium text-[#6E7263]">
              {campaignActive ? '거점 수령과 직거래 모두 가능해요' : '지금은 직거래로 주고받아요'}
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/browse')}
            className="flex items-center gap-0.5 text-[13px] font-semibold text-[#6E7263]"
          >
            전체보기
            <span className="ms text-base">chevron_right</span>
          </button>
        </div>
        <div className="divide-y divide-[var(--color-border)] px-5">
          {previewItems?.map((item) => (
            <ItemCard key={item.id} item={item} onClick={() => navigate(`/items/${item.id}`)} />
          ))}
        </div>
        {(failed || previewItems === null) && (
          <p className="px-5 py-6 text-center text-sm font-medium text-[var(--color-label-alt)]">
            {failed ? '물품을 불러오지 못했어요. 잠시 후 다시 확인해 주세요.' : '물품을 불러오는 중이에요…'}
          </p>
        )}
        {!failed && previewItems?.length === 0 && (
          <EmptyState
            title="장터가 아직 조용해요"
            description="첫 물건을 내놓고 동네 장터를 열어 보세요"
            action={{ label: '물품 등록하기', onClick: () => navigate('/register') }}
          />
        )}
      </div>

      {!loggedIn && <InfoBanner>로그인 없이도 둘러볼 수 있어요. 신청할 때만 로그인이 필요해요</InfoBanner>}
    </div>
  )
}
