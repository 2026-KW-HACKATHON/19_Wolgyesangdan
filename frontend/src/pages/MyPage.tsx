import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { fetchMyImpact, type MyImpact } from '../api/carbonReport'
import MaterialIcon from '../components/icons/MaterialIcon'
import AppliedList from '../components/mypage/AppliedList'
import RegisteredList from '../components/mypage/RegisteredList'
import { useContact } from '../contexts/ContactContext'
import { useLoginGate } from '../hooks/useLoginGate'
import { useMyInfo } from '../hooks/useMyInfo'
import { useMyVerifications } from '../hooks/useMyVerifications'
import type { MyVerification } from '../types/verification'
import { clearTokens, isLoggedIn } from '../lib/authStorage'
import { formatJoinedPeriod, nicknameInitial } from '../lib/profile'

type TabKey = 'registered' | 'applied'

interface VerificationSummary {
  label: string
  className: string
}

/** 우선배정 인증(신입생·기초수급자) 중 하나라도 상태가 있으면 대표 상태로 보여준다. 동네 인증은 세지 않는다. */
function summarizeVerification(verifications: MyVerification[]): VerificationSummary {
  const statuses = verifications.filter((v) => v.verificationType !== 'NEIGHBORHOOD').map((v) => v.status)
  if (statuses.includes('APPROVED')) return { label: '인증 완료', className: 'bg-primary-tint text-primary-tint-ink' }
  if (statuses.includes('PENDING')) return { label: '검토 중', className: 'bg-amber-badge text-amber-badge-ink' }
  return { label: '미인증', className: 'bg-sunken text-ink-2' }
}

/** 내 정보 (GET /users/me). 동네는 GPS 동네 인증이 승인됐을 때만 보여준다. */
function ProfileRow({ verification, neighborhoodVerified }: { verification: VerificationSummary; neighborhoodVerified: boolean }) {
  const { info, loggedIn, loading } = useMyInfo()
  const reviewing = verification.label === '검토 중'
  const name = info ? info.nickname : loading ? '' : loggedIn ? '정보를 불러오지 못했어요' : '로그인이 필요해요'
  const detail = info
    ? `${neighborhoodVerified ? '월계1동' : '동네 인증 전'} · ${formatJoinedPeriod(info.createdAt)}`
    : loading
      ? '불러오는 중…'
      : loggedIn
        ? '잠시 후 다시 확인해 주세요'
        : '로그인하면 내 정보를 볼 수 있어요'
  return (
    <div className="flex items-center gap-3.5 px-5 pt-1 pb-5">
      <span className="flex size-16 flex-none items-center justify-center rounded-full bg-primary-tint text-[24px] font-bold text-accent">
        {info ? nicknameInitial(info.nickname) : <MaterialIcon name="person" size={28} />}
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          <span className="truncate text-[18px] font-extrabold text-label">{name}</span>
          {reviewing && (
            <span className="rounded-[7px] bg-amber-badge px-1.5 py-[3px] text-[11px] font-bold text-amber-badge-ink">
              인증 검토 중
            </span>
          )}
        </div>
        <div className="mt-[3px] text-[13px] font-medium text-label-alt">
          {detail}
        </div>
      </div>
    </div>
  )
}

/** 나의 자원순환 기록 — GET /users/me/impact (거래 완료 기준) */
function RecordCard() {
  const loggedIn = isLoggedIn()
  const [impact, setImpact] = useState<MyImpact | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    if (!loggedIn) return
    let cancelled = false
    fetchMyImpact()
      .then((data) => {
        if (!cancelled) setImpact(data)
      })
      .catch(() => {
        if (!cancelled) setFailed(true)
      })
    return () => {
      cancelled = true
    }
  }, [loggedIn])

  const rows = impact
    ? [
        { title: '전달 완료', count: impact.givenCount },
        { title: '수령 완료', count: impact.receivedCount },
      ]
    : []
  const maxCount = Math.max(...rows.map((row) => row.count), 1)

  return (
    <div className="mx-5 rounded-[18px] bg-primary-tint px-5 py-[18px]">
      <div className="text-[14px] font-bold text-body">나의 자원순환 기록</div>
      {!loggedIn || failed ? (
        <p className="mt-2 text-[13px] font-medium text-primary-tint-ink">
          {loggedIn ? '기록을 불러오지 못했어요. 잠시 후 다시 확인해 주세요.' : '로그인하면 나의 기록을 볼 수 있어요.'}
        </p>
      ) : (
        <>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="text-[32px] font-extrabold tracking-[-0.023em] text-primary-dark">
              {impact ? impact.carbonReductionKg.toLocaleString('ko-KR') : '–'}
            </span>
            <span className="text-[15px] font-bold text-primary-dark">kg CO₂e</span>
            <span className="ml-auto text-[13px] font-semibold text-primary-tint-ink">
              재사용 {impact ? impact.givenCount + impact.receivedCount : '–'}개
            </span>
          </div>
          <div className="mt-0.5 text-[12px] font-medium text-primary-tint-ink opacity-80">
            지금까지 줄인 것으로 예상되는 양이에요
          </div>
          <div className="mt-3.5 flex flex-col gap-[9px]">
            {rows.map((row, i) => (
              <div key={row.title} className="flex items-center gap-2.5">
                <span className="w-18 flex-none text-[13px] font-semibold text-body">{row.title}</span>
                <span className="h-[7px] flex-1 overflow-hidden rounded-full bg-surface">
                  <span
                    // 0에서 제 값까지 자라고, 아래 행일수록 조금씩 늦게 시작한다
                    className="block h-full animate-bar-grow-x rounded-full bg-primary motion-reduce:animate-none"
                    style={{ width: `${(row.count / maxCount) * 100}%`, animationDelay: `${i * 0.08}s` }}
                  />
                </span>
                <span className="w-11 text-right text-[13px] font-bold text-primary-dark">{row.count}회</span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

// 비로그인이면 이동 대신 "로그인이 필요해요" 팝업에 보여줄 안내 (#179)
const VERIFY_LOGIN_MESSAGE = '인증하려면 로그인해 주세요. 로그인하면 바로 인증 화면으로 이어져요.'
const CONTACT_LOGIN_MESSAGE = '연락 수단을 설정하려면 로그인해 주세요. 로그인하면 바로 설정 화면으로 이어져요.'

type GoWithLogin = (to: string, description: string) => void

function VerificationCard({ verification, go }: { verification: VerificationSummary; go: GoWithLogin }) {
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
      <button
        type="button"
        onClick={() => go('/mypage/verification', VERIFY_LOGIN_MESSAGE)}
        className="flex-none rounded-[10px] bg-primary px-3 py-2 text-[13px] font-bold text-screen"
      >
        인증하기
      </button>
    </div>
  )
}

function MenuRows({ go }: { go: GoWithLogin }) {
  const { contact } = useContact()
  const contactLabel = contact ? (contact.type === 'openchat' ? '오픈채팅방' : '전화번호') : '미설정'
  const rows: { label: string; value?: string; onClick: () => void }[] = [
    { label: '연락 수단 설정', value: contactLabel, onClick: () => go('/settings/contact?next=/mypage', CONTACT_LOGIN_MESSAGE) },
    { label: '인증하기', value: '주민 · 학생 · 우선배정', onClick: () => go('/mypage/verification', VERIFY_LOGIN_MESSAGE) },
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
  const navigate = useNavigate()
  const loggedIn = isLoggedIn()
  const { go, dialog: loginDialog } = useLoginGate()

  // 로그아웃 API는 MVP에서 만들지 않음 — 이 기기의 토큰만 지우고 홈으로 간다
  const handleLogout = () => {
    if (!window.confirm('로그아웃할까요?')) return
    clearTokens()
    navigate('/', { replace: true })
  }
  const { verifications } = useMyVerifications()
  const verification = summarizeVerification(verifications)
  const neighborhoodVerified = verifications.some(
    (v) => v.verificationType === 'NEIGHBORHOOD' && v.status === 'APPROVED',
  )

  const changeTab = (next: TabKey) => {
    setSearchParams(next === 'applied' ? { tab: 'applied' } : {}, { replace: true })
  }

  const tabs: { key: TabKey; label: string }[] = [
    { key: 'registered', label: '내가 등록한 물품' },
    { key: 'applied', label: '내가 신청한 물품' },
  ]

  return (
    <div className="flex flex-col pb-4">
      <header className="flex h-14 items-center px-5">
        <h1 className="flex-1 font-hand text-[24px] font-bold text-label">마이페이지</h1>
        {loggedIn && (
          <button
            type="button"
            onClick={handleLogout}
            className="flex cursor-pointer items-center gap-1 rounded-full px-2 py-1.5 text-[13px] font-semibold text-ink-2"
          >
            <MaterialIcon name="logout" size={18} />
            로그아웃
          </button>
        )}
      </header>

      <ProfileRow verification={verification} neighborhoodVerified={neighborhoodVerified} />
      <RecordCard />
      <VerificationCard verification={verification} go={go} />

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
            <RegisteredList />
            <div className="mt-2 h-2 bg-sunken" />
            <MenuRows go={go} />
          </>
        ) : (
          <AppliedList />
        )}
      </div>
      {loginDialog}
    </div>
  )
}
