import { useNavigate } from 'react-router-dom'
import CampaignBanner from '../components/CampaignBanner'
import SearchBar from '../components/SearchBar'
import ItemCard from '../components/ItemCard'
import InfoBanner from '../components/InfoBanner'
import { ITEMS } from '../data/items'

const CATEGORIES = [
  { key: '가구', icon: 'chair' },
  { key: '가전', icon: 'bolt' },
  { key: '주방', icon: 'local_cafe' },
  { key: '생활', icon: 'home' },
  { key: '기타', icon: 'more_horiz' },
]

export default function Home() {
  const navigate = useNavigate()
  const previewItems = ITEMS.slice(0, 4)

  return (
    <div className="flex flex-col gap-5 pb-6">
      <header className="flex items-center gap-2 px-5 pt-3.5">
        <span className="text-2xl font-bold text-[var(--color-accent)]">월계상단</span>
        <span className="rounded-md bg-[#EBE4D1] px-2 py-0.5 text-xs font-semibold text-[var(--color-label-alt)]">
          월계1동
        </span>
        <span className="flex-1" />
        <button type="button" className="text-[13px] font-bold text-[var(--color-accent)]">
          로그인
        </button>
      </header>

      <CampaignBanner
        name="2026 자원순환 캠페인"
        period="9.20 ~ 10.4"
        hub="광운대 비마관 1층 거점"
        ddayLabel="종료까지 D-12"
        reusedCount={128}
        carbonKg={3420}
      />

      <div className="px-5">
        <SearchBar onClick={() => navigate('/browse')} />
      </div>

      <div className="grid grid-cols-5 px-3">
        {CATEGORIES.map((c) => (
          <button
            key={c.key}
            type="button"
            onClick={() => navigate('/browse')}
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
              거점 수령과 직거래 모두 가능해요
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
          {previewItems.map((item) => (
            <ItemCard key={item.id} item={item} onClick={() => navigate(`/items/${item.id}`)} />
          ))}
        </div>
      </div>

      <InfoBanner>로그인 없이도 둘러볼 수 있어요. 신청할 때만 로그인이 필요해요</InfoBanner>
    </div>
  )
}
