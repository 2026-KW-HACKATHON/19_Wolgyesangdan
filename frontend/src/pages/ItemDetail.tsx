import { useNavigate, useParams } from 'react-router-dom'
import Badge from '../components/Badge'
import { ITEMS, STATUS_LABEL, TRADE_METHOD_LABEL } from '../data/items'
import { CAMPAIGN } from '../data/campaign'

function SpecRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-3 py-1.5">
      <span className="w-20 flex-none text-sm font-medium text-[var(--color-label-alt)]">{label}</span>
      <span className="flex-1 text-sm font-semibold text-[var(--color-label)]">{value}</span>
    </div>
  )
}

export default function ItemDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const item = ITEMS.find((i) => i.id === id)

  if (!item) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
        <span className="ms text-4xl text-[var(--color-label-alt)]">search_off</span>
        <p className="text-base font-bold text-[var(--color-label)]">물품을 찾을 수 없어요</p>
        <button
          type="button"
          onClick={() => navigate('/browse')}
          className="rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-bold text-[var(--color-surface)]"
        >
          둘러보기로 돌아가기
        </button>
      </div>
    )
  }

  const statusTone = item.status === 'OPEN' ? 'primary' : item.status === 'ASSIGNING' ? 'warning' : 'neutral'

  return (
    <div className="flex h-full flex-col">
      <header className="flex h-14 flex-none items-center gap-1.5 px-3.5">
        <button type="button" onClick={() => navigate(-1)} className="flex h-9 w-9 items-center justify-center text-[var(--color-label)]">
          <span className="ms text-2xl">chevron_left</span>
        </button>
        <h1 className="flex-1 truncate text-[17px] font-bold text-[var(--color-label)]">{item.name}</h1>
        <span className="ms mr-1 text-xl text-[#4A4A40]">ios_share</span>
      </header>

      <div className="flex-1 overflow-y-auto">
        <div className="flex h-[220px] items-center justify-center bg-[#E7EBD8] text-[var(--color-primary)]">
          <span className="ms text-[78px]">{item.icon}</span>
        </div>

        <div className="flex flex-col gap-4 px-5 pt-4 pb-6">
          <div>
            <div className="mb-2 flex flex-wrap gap-1.5">
              <Badge tone={statusTone}>{STATUS_LABEL[item.status]}</Badge>
              {item.tradeMethods.map((method) => (
                <Badge key={method}>{TRADE_METHOD_LABEL[method]}</Badge>
              ))}
            </div>
            <h2 className="text-2xl font-bold text-[var(--color-label)]">{item.name}</h2>
            <p className="mt-1 text-sm font-medium text-[var(--color-label-alt)]">
              {item.categoryGroup} · {item.category} · {item.condition}
            </p>
          </div>

          <div className="flex items-center gap-3.5 rounded-2xl bg-[#E7EBD8] px-4.5 py-4">
            <span className="flex h-10 w-10 flex-none items-center justify-center rounded-2xl bg-[var(--color-surface)] text-[var(--color-accent)]">
              <span className="ms text-xl">autorenew</span>
            </span>
            <div>
              <div className="flex items-baseline gap-1">
                <span className="text-[28px] font-extrabold text-[var(--color-primary-dark)]">
                  {item.carbonKg}
                </span>
                <span className="text-[15px] font-bold text-[var(--color-primary-dark)]">kg CO₂e 절감</span>
              </div>
              <p className="mt-0.5 text-xs font-medium text-[#57603F]">
                재사용 시 예상치예요. 실제 절감량과 다를 수 있어요
              </p>
            </div>
          </div>

          <section>
            <h3 className="mb-1 text-lg font-bold text-[var(--color-label)]">물품 정보</h3>
            <SpecRow label="사용 기간" value={item.usagePeriod} />
            <SpecRow label="하자 여부" value={item.defect} />
            <SpecRow label="작동 여부" value={item.workingStatus} />
            <SpecRow label="크기" value={item.size} />
            <SpecRow label="운반 난이도" value={item.transportDifficulty} />
          </section>

          <p className="text-[15px] leading-relaxed font-medium text-[var(--color-body)]">
            {item.description}
          </p>

          <div className="h-px bg-[var(--color-border)]" />

          <section>
            <h3 className="mb-1 text-lg font-bold text-[var(--color-label)]">전달 방법</h3>
            <SpecRow label="전달 가능" value={item.deadlineLabel} />
            <SpecRow
              label="거래 방식"
              value={item.tradeMethods.map((m) => TRADE_METHOD_LABEL[m]).join(' · ')}
            />
            {item.tradeMethods.includes('CAMPAIGN') && (
              <div className="mt-2 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3.5">
                <div className="flex items-center gap-1.5 text-sm font-bold text-[var(--color-label)]">
                  <span className="ms text-base text-[var(--color-primary)]">location_on</span>
                  {CAMPAIGN.hubName}
                </div>
                <p className="mt-1 text-xs font-medium text-[var(--color-label-alt)]">
                  {CAMPAIGN.hubHours} · 비대면 수령
                </p>
              </div>
            )}
          </section>

          <div className="flex items-center gap-2.5 pt-1">
            <span className="flex h-10 w-10 flex-none items-center justify-center rounded-full bg-[#EBE4D1] text-base font-bold text-[#4A4A40]">
              {item.ownerName.slice(0, 1)}
            </span>
            <div className="flex-1">
              <div className="text-sm font-bold text-[var(--color-label)]">{item.ownerName}</div>
              <div className="mt-0.5 text-xs font-medium text-[var(--color-label-alt)]">
                전달 완료 {item.ownerCompletedCount}회
              </div>
            </div>
            <span className="text-[13px] font-medium text-[var(--color-label-alt)]">
              대기 {item.applicantCount} / {item.maxApplicants}명
            </span>
          </div>
        </div>
      </div>

      <div className="flex flex-none items-center gap-3 border-t border-[var(--color-border)] bg-[var(--color-surface)] px-5 py-3">
        <button
          type="button"
          className="flex h-12 w-12 flex-none items-center justify-center rounded-2xl border border-[var(--color-border)] text-[#4A4A40]"
        >
          <span className="ms text-xl">favorite</span>
        </button>
        <button
          type="button"
          className="h-12 flex-1 rounded-2xl bg-[var(--color-primary)] text-base font-bold text-[var(--color-surface)]"
        >
          신청하기
        </button>
      </div>
    </div>
  )
}
