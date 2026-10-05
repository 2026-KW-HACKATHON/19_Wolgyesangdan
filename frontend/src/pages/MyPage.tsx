import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import MaterialIcon from '../components/icons/MaterialIcon'
import {
  APPLIED_ITEMS,
  MY_RECORD,
  PROFILE,
  REGISTERED_ITEMS,
  VERIFICATION,
  type AppliedItem,
  type RegisteredItem,
  type VerificationStatus,
} from '../data/mypage'

type TabKey = 'registered' | 'applied'

/** 학생·주민·저소득층 인증 중 하나라도 상태가 있으면 대표 상태로 보여준다. */
function summarizeVerification(): { label: string; className: string } {
  const statuses: VerificationStatus[] = [
    VERIFICATION.student.status,
    VERIFICATION.resident.status,
    VERIFICATION.lowIncome.status,
  ]
  if (statuses.includes('approved')) return { label: '인증 완료', className: 'bg-primary-tint text-primary-tint-ink' }
  if (statuses.includes('submitted') || statuses.includes('reviewing'))
    return { label: '검토 중', className: 'bg-amber-badge text-amber-badge-ink' }
  return { label: '미인증', className: 'bg-sunken text-ink-2' }
}

function ProfileRow() {
  const verification = summarizeVerification()
  const reviewing = verification.label === '검토 중'
  return (
    <div className="flex items-center gap-3.5 px-5 pt-1 pb-5">
      <span className="flex size-16 flex-none items-center justify-center rounded-full bg-primary-tint text-[24px] font-bold text-accent">
        {PROFILE.initial}
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          <span className="text-[18px] font-extrabold text-label">{PROFILE.name}</span>
          {reviewing && (
            <span className="rounded-[7px] bg-amber-badge px-1.5 py-[3px] text-[11px] font-bold text-amber-badge-ink">
              인증 검토 중
            </span>
          )}
        </div>
        <div className="mt-[3px] text-[13px] font-medium text-label-alt">
          {PROFILE.dong} · 가입 {PROFILE.joinedMonths}개월
        </div>
      </div>
      <button
        type="button"
        className="flex-none cursor-pointer rounded-full border border-border bg-surface px-3 py-[7px] text-[13px] font-semibold whitespace-nowrap text-ink-2"
      >
        프로필 수정
      </button>
    </div>
  )
}

function RecordCard() {
  const maxKg = Math.max(...MY_RECORD.byItem.map((i) => i.kg), 1)
  return (
    <div className="mx-5 rounded-[18px] bg-primary-tint px-5 py-[18px]">
      <div className="text-[14px] font-bold text-body">나의 자원순환 기록</div>
      <div className="mt-2 flex items-baseline gap-1.5">
        <span className="text-[32px] font-extrabold tracking-[-0.023em] text-primary-dark">{MY_RECORD.co2eTotalKg}</span>
        <span className="text-[15px] font-bold text-primary-dark">kg CO₂e</span>
        <span className="ml-auto text-[13px] font-semibold text-primary-tint-ink">재사용 {MY_RECORD.reusedCount}개</span>
      </div>
      <div className="mt-0.5 text-[12px] font-medium text-primary-tint-ink opacity-80">
        지금까지 줄인 것으로 예상되는 양이에요
      </div>
      <div className="mt-3.5 flex flex-col gap-[9px]">
        {MY_RECORD.byItem.map((item, i) => (
          <div key={item.title} className="flex items-center gap-2.5">
            <span className="w-18 flex-none text-[13px] font-semibold text-body">{item.title}</span>
            <span className="h-[7px] flex-1 overflow-hidden rounded-full bg-surface">
              <span
                // 0에서 제 값까지 자라고, 아래 행일수록 조금씩 늦게 시작한다
                className="block h-full animate-bar-grow-x rounded-full bg-primary motion-reduce:animate-none"
                style={{ width: `${(item.kg / maxKg) * 100}%`, animationDelay: `${i * 0.08}s` }}
              />
            </span>
            <span className="w-11 text-right text-[13px] font-bold text-primary-dark">{item.kg}kg</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function VerificationCard() {
  const verification = summarizeVerification()
  return (
    <div className="mx-5 mt-3 flex items-center gap-3 rounded-[18px] border border-border bg-surface px-[18px] py-4">
      <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-sunken text-ink-2">
        <MaterialIcon name="verified" size={20} />
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          <span className="text-[15px] font-bold text-label">우선배정 인증</span>
          <span className={`rounded-[7px] px-2 py-0.5 text-[11px] font-semibold ${verification.className}`}>
            {verification.label}
          </span>
        </div>
        <div className="mt-[3px] text-[12px] font-medium text-label-alt">
          신입생·저소득층이라면 인증하고 가산점을 받으세요
        </div>
      </div>
      <Link
        to="/mypage/verification"
        className="flex-none rounded-[10px] bg-primary px-3 py-2 text-[13px] font-bold text-screen"
      >
        인증하기
      </Link>
    </div>
  )
}

function RegisteredList({ items }: { items: RegisteredItem[] }) {
  const navigate = useNavigate()
  return (
    <div className="px-5">
      {items.map((item, i) => (
        <div key={item.id}>
          <button
            type="button"
            onClick={() => navigate(`/items/${item.id}`)}
            className="flex w-full cursor-pointer items-center gap-3 py-3.5 text-left"
          >
            <span className={`flex size-16 flex-none items-center justify-center rounded-[14px] ${item.iconClassName}`}>
              <MaterialIcon name={item.icon} size={26} />
            </span>
            <span className="min-w-0 flex-1">
              <span
                className={`inline-block rounded-[7px] px-[7px] py-[3px] text-[11px] font-bold ${
                  item.status === 'OPEN' ? 'bg-primary-tint text-primary-dark' : 'bg-sunken text-ink-2'
                }`}
              >
                {item.status === 'OPEN' ? '신청자 모집 중' : '전달 완료'}
              </span>
              <span className="mt-[5px] block text-[15px] font-bold text-label">{item.title}</span>
              <span className="mt-px block text-[13px] font-medium text-label-alt">{item.meta}</span>
            </span>
            <MaterialIcon name="chevron_right" size={18} className="text-label-alt" />
          </button>
          {i < items.length - 1 && <div className="h-px bg-border" />}
        </div>
      ))}
    </div>
  )
}

/** 대기 순번 5칸 바. 내 순번 이전 칸은 진하게, 내 칸은 중간 톤으로 표시한다. */
function WaitlistBar({ myNo, total }: { myNo: number; total: number }) {
  return (
    <div className="mt-2.5 flex gap-1">
      {Array.from({ length: total }, (_, i) => {
        const cls =
          i < myNo - 1 ? 'bg-primary' : i === myNo - 1 ? 'bg-primary-mid' : 'bg-surface'
        return <span key={i} className={`h-[7px] flex-1 rounded-full ${cls}`} />
      })}
    </div>
  )
}

function ContactTile({ contact }: { contact: NonNullable<AppliedItem['contact']> }) {
  if (contact.type === 'openchat') {
    return (
      <div className="mt-2.5 flex items-center gap-[9px] rounded-[14px] border border-border bg-surface px-3.5 py-3">
        <span className="flex size-[34px] flex-none items-center justify-center rounded-[11px] bg-kakao text-kakao-ink">
          <MaterialIcon name="chat_bubble" size={18} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-[14px] font-bold text-label">카카오 오픈채팅방</div>
          <div className="truncate text-[12px] font-medium text-label-alt">{contact.value}</div>
        </div>
        <a
          href={`https://${contact.value}`}
          target="_blank"
          rel="noreferrer"
          className="flex-none rounded-[10px] bg-primary px-2.5 py-2 text-[12px] font-bold text-screen"
        >
          열기
        </a>
      </div>
    )
  }
  return (
    <div className="mt-2.5 flex items-center gap-[9px] rounded-[14px] border border-border bg-surface px-3.5 py-3">
      <MaterialIcon name="call" size={18} className="text-accent" />
      <span className="flex-1 text-[14px] font-bold text-label">{contact.value}</span>
    </div>
  )
}

function AppliedList({ items, onCancel }: { items: AppliedItem[]; onCancel: (id: string) => void }) {
  const navigate = useNavigate()

  if (items.length === 0) {
    return <p className="px-5 py-10 text-center text-[14px] font-medium text-label-alt">아직 신청한 물품이 없어요</p>
  }

  return (
    <div className="flex flex-col gap-3 px-5">
      {items.map((item) => {
        const assigned = item.status === 'ASSIGNED'
        const done = item.status === 'DONE'
        return (
          <div
            key={item.id}
            className={`rounded-[20px] bg-surface p-3.5 ${
              assigned ? 'border-2 border-primary' : 'border border-border'
            } ${done ? 'opacity-85' : ''}`}
          >
            <div className="flex gap-3">
              <span className={`flex size-[72px] flex-none items-center justify-center rounded-2xl ${item.iconClassName}`}>
                <MaterialIcon name={item.icon} size={30} />
              </span>
              <div className="min-w-0 flex-1">
                <span
                  className={`inline-block rounded-[7px] px-[7px] py-[3px] text-[11px] font-bold ${
                    assigned
                      ? 'bg-primary text-screen'
                      : done
                        ? 'bg-sunken text-ink-2'
                        : 'bg-amber-badge text-amber-badge-ink'
                  }`}
                >
                  {assigned ? '나에게 배정됨' : done ? '전달 완료' : '배정 중'}
                </span>
                <div className="mt-1.5 text-[16px] font-bold text-label">{item.title}</div>
                <div className="mt-0.5 text-[13px] font-medium text-label-alt">
                  {done && item.co2eKg ? `${item.meta} · ${item.co2eKg}kg CO₂e 절감` : item.meta}
                </div>
              </div>
            </div>

            {item.status === 'WAITING' && item.waitlistNo && item.waitlistCount && (
              <div className="mt-3 rounded-[14px] bg-primary-tint px-3.5 py-[13px]">
                <div className="flex items-baseline gap-1.5">
                  <span className="text-[14px] font-bold text-body">대기</span>
                  <span className="text-[26px] font-extrabold tracking-[-0.02em] text-primary-dark">{item.waitlistNo}</span>
                  <span className="text-[15px] font-bold text-primary-dark">번</span>
                  <span className="ml-auto text-[13px] font-semibold text-primary-tint-ink">
                    신청자 {item.waitlistCount}명 중
                  </span>
                </div>
                <WaitlistBar myNo={item.waitlistNo} total={item.waitlistCount} />
                <p className="mt-2.5 text-[12px] leading-normal font-medium text-primary-tint-ink">
                  {item.waitlistNo === item.waitlistCount
                    ? '대기는 5번까지만 받아요. 마지막 순번이라 배정이 어려울 수 있어요.'
                    : '앞 순번이 취소하면 자동으로 올라가요. 우선배정 인증을 하면 순번이 앞당겨질 수 있어요.'}
                </p>
              </div>
            )}

            {assigned && (
              <>
                <div className="mt-3 flex items-center gap-2 rounded-[14px] bg-primary-tint px-3.5 py-3">
                  <MaterialIcon name="check_circle" size={19} className="flex-none text-accent" />
                  <span className="text-[13px] font-semibold text-primary-tint-ink">
                    대기 {item.waitlistNo}번이었어요. 등록자와 약속을 잡아주세요.
                  </span>
                </div>
                {item.contact && (
                  <div className="mt-2.5 rounded-[14px] border border-border bg-surface px-3.5 py-[13px]">
                    <div className="text-[12px] font-bold text-label-alt">등록자가 선택한 연락 수단</div>
                    <ContactTile contact={item.contact} />
                  </div>
                )}
              </>
            )}

            {item.status !== 'DONE' && (
              <div className="mt-2.5 flex gap-2">
                {item.status === 'WAITING' && (
                  <button
                    type="button"
                    onClick={() => onCancel(item.id)}
                    className="flex-1 cursor-pointer rounded-xl border border-border py-2.5 text-[13px] font-bold text-ink-2"
                  >
                    신청 취소
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => navigate(`/items/${item.id}`)}
                  className="flex-1 cursor-pointer rounded-xl bg-primary py-2.5 text-[13px] font-bold text-screen"
                >
                  물품 보기
                </button>
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}

function MenuRows() {
  const navigate = useNavigate()
  const rows: { label: string; value?: string; onClick: () => void }[] = [
    { label: '연락 수단 설정', value: '오픈채팅방', onClick: () => navigate('/settings/contact?next=/mypage') },
    { label: '인증하기', value: '주민 · 학생 · 우선배정', onClick: () => navigate('/mypage/verification') },
    { label: '이용 안내', onClick: () => {} },
    { label: '문의하기', onClick: () => {} },
  ]
  return (
    <div className="mt-4 border-t border-border">
      {rows.map((row) => (
        <button
          key={row.label}
          type="button"
          onClick={row.onClick}
          className="flex w-full cursor-pointer items-center gap-2.5 border-b border-border px-5 py-4 text-left text-[15px] font-semibold text-label"
        >
          {row.label}
          {row.value && <span className="ml-auto text-[13px] font-semibold text-label-alt">{row.value}</span>}
          <MaterialIcon name="chevron_right" size={18} className={`text-label-alt ${row.value ? '' : 'ml-auto'}`} />
        </button>
      ))}
    </div>
  )
}

/** 마이페이지 (탭 5, /mypage). 6a 기본 + 6c 내가 신청한 물품 탭. 로그인 필요. */
export default function MyPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const tab: TabKey = searchParams.get('tab') === 'applied' ? 'applied' : 'registered'
  const [applied, setApplied] = useState<AppliedItem[]>(APPLIED_ITEMS)

  const changeTab = (next: TabKey) => {
    setSearchParams(next === 'applied' ? { tab: 'applied' } : {}, { replace: true })
  }

  const handleCancel = (id: string) => {
    // TODO: DELETE /applications/:id 연동. 취소하면 뒤 순번이 자동으로 당겨진다.
    if (!window.confirm('신청을 취소할까요?')) return
    setApplied((prev) => prev.filter((item) => item.id !== id))
  }

  const tabs: { key: TabKey; label: string }[] = [
    { key: 'registered', label: '내가 등록한 물품' },
    { key: 'applied', label: '내가 신청한 물품' },
  ]

  return (
    <div className="flex flex-col pb-4">
      <header className="flex h-14 items-center px-5">
        <h1 className="flex-1 font-hand text-[24px] font-bold text-label">마이페이지</h1>
        <MaterialIcon name="settings" size={22} className="text-ink-2" />
      </header>

      <ProfileRow />
      <RecordCard />
      <VerificationCard />

      <div role="tablist" className="mt-5 flex border-b border-border">
        {tabs.map((t) => {
          const selected = tab === t.key
          return (
            <button
              key={t.key}
              type="button"
              role="tab"
              aria-selected={selected}
              onClick={() => changeTab(t.key)}
              className={`flex-1 cursor-pointer py-3 text-center text-[15px] ${
                selected ? 'font-bold text-accent shadow-[inset_0_-2px_0_0_var(--color-primary)]' : 'font-medium text-label-alt'
              }`}
            >
              {t.label}
            </button>
          )
        })}
      </div>

      <div className="pt-3.5">
        {tab === 'registered' ? (
          <>
            <RegisteredList items={REGISTERED_ITEMS} />
            <div className="mt-2 h-2 bg-sunken" />
            <MenuRows />
          </>
        ) : (
          <AppliedList items={applied} onCancel={handleCancel} />
        )}
      </div>
    </div>
  )
}
