import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { getAdminInquiries } from '../api/inquiries'
import { getRecentVerifications } from '../api/summary'
import Card from '../components/Card'
import MaterialIcon from '../components/MaterialIcon'
import StatusBadge, { type StatusTone } from '../components/StatusBadge'
import { useAdminSummary } from '../lib/summaryContext'
import type { AdminInquirySummary, InquiryCategory } from '../types/inquiry'
import type { RecentVerification } from '../types/summary'

const RECENT_SIZE = 3

const VERIFICATION_TYPE_LABEL: Record<RecentVerification['verificationType'], string> = {
  FRESHMAN: '신입생',
  LOW_INCOME: '기초수급자',
}

const VERIFICATION_STATUS: Record<RecentVerification['status'], { label: string; tone: StatusTone }> = {
  PENDING: { label: '검토 대기', tone: 'waiting' },
  APPROVED: { label: '승인', tone: 'done' },
  REJECTED: { label: '반려', tone: 'rejected' },
  EXPIRED: { label: '만료', tone: 'muted' },
}

const INQUIRY_CATEGORY_LABEL: Record<InquiryCategory, string> = {
  TRADE: '거래',
  VERIFICATION: '인증',
  HUB: '거점',
  ETC: '기타',
}

/** YYYY-MM-DDTHH:mm:ss → "2026.10.08" */
function formatDate(dateTime: string) {
  return dateTime.slice(0, 10).split('-').join('.')
}

interface SummaryCardProps {
  label: string
  /** 아직 못 불러왔으면 null — "–"로 보여 준다 */
  value: number | null
  unit: string
  icon: string
  /** 주면 눌러서 그 화면으로 간다 */
  to?: string
  /** 숫자 아래 한 줄 (캠페인 이름 등) */
  note?: string
  tinted?: boolean
}

function SummaryCard({ label, value, unit, icon, to, note, tinted }: SummaryCardProps) {
  const body = (
    <>
      <div className="flex items-center gap-1.5 text-[13px] font-bold text-label-alt">
        <MaterialIcon name={icon} size={18} />
        {label}
        {to && <MaterialIcon name="chevron_right" size={18} className="ml-auto" />}
      </div>
      <p className="mt-3 text-[30px] leading-none font-extrabold text-label">
        {value === null ? '–' : value.toLocaleString()}
        {value !== null && <span className="ml-1 text-[14px] font-bold text-label-alt">{unit}</span>}
      </p>
      {note && <p className="mt-2 truncate text-[12px] text-label-alt">{note}</p>}
    </>
  )
  const className = `block rounded-2xl border border-border p-5 ${tinted ? 'bg-primary-tint' : 'bg-surface'}`
  return to ? (
    <Link to={to} className={`${className} hover:border-border-deep`}>
      {body}
    </Link>
  ) : (
    <div className={className}>{body}</div>
  )
}

/** 최근 목록 한 칸의 상태 — 불러오는 중(null) / 실패 / 목록 */
type Recent<T> = T[] | 'failed' | null

interface RecentCardProps<T> {
  title: string
  to: string
  rows: Recent<T>
  emptyMessage: string
  rowKey: (row: T) => number
  render: (row: T) => ReactNode
}

function RecentCard<T>({ title, to, rows, emptyMessage, rowKey, render }: RecentCardProps<T>) {
  return (
    <Card>
      <div className="flex items-center justify-between px-5 pt-5 pb-3">
        <h2 className="text-[15px] font-extrabold text-label">{title}</h2>
        <Link to={to} className="flex items-center text-[13px] font-bold text-accent">
          전체 보기
          <MaterialIcon name="chevron_right" size={16} />
        </Link>
      </div>
      {rows === null ? (
        <p className="px-5 pb-6 text-[13px] text-label-alt">불러오는 중이에요…</p>
      ) : rows === 'failed' ? (
        <p role="alert" className="px-5 pb-6 text-[13px] text-label-alt">
          불러오지 못했어요. 잠시 후 다시 시도해 주세요.
        </p>
      ) : rows.length === 0 ? (
        <p className="px-5 pb-6 text-[13px] text-label-alt">{emptyMessage}</p>
      ) : (
        <ul>
          {rows.map((row) => (
            <li key={rowKey(row)} className="border-t border-row-line px-5 py-3.5">
              {render(row)}
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

/** 대시보드 (#217) — 오늘 처리할 일 요약. 요약 숫자는 사이드바 배지 · 상단 바 칩과 같은 데이터(AdminLayout)를 쓴다 */
export default function DashboardPage() {
  const { summary, failed } = useAdminSummary()
  const [verifications, setVerifications] = useState<Recent<RecentVerification>>(null)
  const [inquiries, setInquiries] = useState<Recent<AdminInquirySummary>>(null)

  // 최근 3건은 별도 API 없이 각 목록 API를 size=3으로 부른다
  useEffect(() => {
    let ignore = false
    getRecentVerifications(RECENT_SIZE)
      .then((page) => {
        if (!ignore) setVerifications(page.content)
      })
      .catch(() => {
        if (!ignore) setVerifications('failed')
      })
    getAdminInquiries(undefined, 0, RECENT_SIZE)
      .then((page) => {
        if (!ignore) setInquiries(page.content)
      })
      .catch(() => {
        if (!ignore) setInquiries('failed')
      })
    return () => {
      ignore = true
    }
  }, [])

  const campaign = summary?.currentCampaign ?? null

  return (
    <div className="flex flex-col gap-5">
      {failed && !summary && (
        <p role="alert" className="text-[13px] font-semibold text-terracotta">
          요약을 불러오지 못했어요. 잠시 후 새로고침해 주세요.
        </p>
      )}

      <div className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-4">
        <SummaryCard
          label="검토 대기 서류"
          value={summary?.pendingVerifications ?? null}
          unit="건"
          icon="fact_check"
          to="/verifications"
        />
        <SummaryCard
          label="답변 대기 문의"
          value={summary?.openInquiries ?? null}
          unit="건"
          icon="forum"
          to="/inquiries"
        />
        <SummaryCard label="숨긴 물품" value={summary?.hiddenItems ?? null} unit="개" icon="inventory_2" to="/items" />
        <SummaryCard
          label="이번 캠페인 거래"
          value={summary ? (campaign?.reusedCount ?? null) : null}
          unit="건"
          icon="campaign"
          note={summary ? (campaign ? campaign.name : '진행 중인 캠페인이 없어요') : undefined}
          tinted
        />
      </div>

      <div className="grid grid-cols-2 items-start gap-5">
        <RecentCard
          title="최근 들어온 서류"
          to="/verifications"
          rows={verifications}
          emptyMessage="들어온 서류가 없어요"
          rowKey={(verification) => verification.id}
          render={(verification) => (
            <div className="flex items-center gap-3">
              <div className="min-w-0 flex-1">
                <p className="truncate text-[14px] font-bold text-label">{verification.nickname}</p>
                <p className="mt-0.5 text-[12px] text-label-alt">
                  {VERIFICATION_TYPE_LABEL[verification.verificationType]} · {formatDate(verification.submittedAt)}
                </p>
              </div>
              <StatusBadge tone={VERIFICATION_STATUS[verification.status].tone}>
                {VERIFICATION_STATUS[verification.status].label}
              </StatusBadge>
            </div>
          )}
        />
        <RecentCard
          title="최근 문의"
          to="/inquiries"
          rows={inquiries}
          emptyMessage="들어온 문의가 없어요"
          rowKey={(inquiry) => inquiry.id}
          render={(inquiry) => (
            <div className="flex items-center gap-3">
              <div className="min-w-0 flex-1">
                <p className="truncate text-[14px] font-bold text-label">{inquiry.title}</p>
                <p className="mt-0.5 text-[12px] text-label-alt">
                  {INQUIRY_CATEGORY_LABEL[inquiry.category]} · {inquiry.nickname} · {formatDate(inquiry.createdAt)}
                </p>
              </div>
              <StatusBadge tone={inquiry.status === 'OPEN' ? 'waiting' : 'done'}>
                {inquiry.status === 'OPEN' ? '답변 대기' : '답변 완료'}
              </StatusBadge>
            </div>
          )}
        />
      </div>
    </div>
  )
}
