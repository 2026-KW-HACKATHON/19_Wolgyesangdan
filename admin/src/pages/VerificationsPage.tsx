import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import {
  approveAdminVerification,
  getAdminVerification,
  getAdminVerificationFile,
  getAdminVerifications,
  rejectAdminVerification,
} from '../api/verifications'
import Card from '../components/Card'
import DataTable, { type Column } from '../components/DataTable'
import FilterChips from '../components/FilterChips'
import MaterialIcon from '../components/MaterialIcon'
import StatusBadge, { type StatusTone } from '../components/StatusBadge'
import type { PageResponse } from '../types/page'
import type {
  AdminVerificationDetail,
  AdminVerificationFile,
  AdminVerificationSummary,
  DocumentType,
  VerificationStatus,
  VerificationType,
} from '../types/verification'

const TYPE_LABEL: Record<VerificationType, string> = {
  FRESHMAN: '신입생',
  LOW_INCOME: '기초수급자',
}

const DOCUMENT_LABEL: Record<DocumentType, string> = {
  ADMISSION_LETTER: '합격증',
  STUDENT_ID_CARD: '학생증',
  RECIPIENT_CERTIFICATE: '수급자 증명서',
}

const STATUS: Record<VerificationStatus, { label: string; tone: StatusTone }> = {
  PENDING: { label: '검토 대기', tone: 'waiting' },
  APPROVED: { label: '승인', tone: 'done' },
  REJECTED: { label: '반려', tone: 'rejected' },
  EXPIRED: { label: '만료', tone: 'muted' },
}

/** 서류에서 확인할 것 (시안 기준) */
const CHECKLIST: Record<VerificationType, string> = {
  FRESHMAN: '광운대학교 · 본인 이름 · 올해 입학',
  LOW_INCOME: '본인 이름 · 발급일 3개월 이내',
}

const REJECT_REASONS = [
  '사진이 흐려 내용을 읽을 수 없어요',
  '이름이 보이지 않아요',
  '유효기간이 지난 서류예요',
  '해당 유형의 서류가 아니에요',
]

type Filter = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL'

const FILTER_OPTIONS: { value: Filter; label: string }[] = [
  { value: 'PENDING', label: '검토 대기' },
  { value: 'APPROVED', label: '승인' },
  { value: 'REJECTED', label: '반려' },
  { value: 'ALL', label: '전체' },
]

/** YYYY-MM-DDTHH:mm:ss → "10.5" */
function monthDay(dateTime: string) {
  const [, month, day] = dateTime.slice(0, 10).split('-').map(Number)
  return `${month}.${day}`
}

/** YYYY-MM-DDTHH:mm:ss → "2027.12.31" */
function fullDate(dateTime: string) {
  return dateTime.slice(0, 10).split('-').join('.')
}

function VerificationStatusBadge({ status }: { status: VerificationStatus }) {
  return <StatusBadge tone={STATUS[status].tone}>{STATUS[status].label}</StatusBadge>
}

/** 서류가 지워졌으면 이름도 함께 지워져서 닉네임으로 대신한다 */
function displayName(verification: AdminVerificationSummary) {
  return verification.applicantName ?? verification.nickname
}

const errorMessage = (e: unknown, fallback: string) => (e instanceof ApiError ? e.message : fallback)

type FileResult = { data: AdminVerificationFile } | { data: null; message: string }

/** 서류 미리보기 300px — 열 때마다 5분짜리 주소를 받는다 (서버에 열람 기록이 남는다) */
function DocumentPreview({ verificationId, hasDocument }: { verificationId: number; hasDocument: boolean }) {
  const [result, setResult] = useState<FileResult | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!hasDocument) return
    let ignore = false
    getAdminVerificationFile(verificationId)
      .then((data) => {
        if (!ignore) setResult({ data })
      })
      .catch((e: unknown) => {
        if (!ignore) setResult({ data: null, message: errorMessage(e, '서류를 불러오지 못했어요.') })
      })
    return () => {
      ignore = true
    }
  }, [verificationId, hasDocument, reloadKey])

  const frame = 'flex h-[300px] flex-col items-center justify-center gap-2 rounded-xl bg-sunken text-center'

  if (!hasDocument) {
    return (
      <div className={frame}>
        <MaterialIcon name="description" size={32} className="text-label-alt" />
        <p className="text-[13px] font-medium text-label-alt">서류 파일이 없어요 (검토 후 30일이 지나 삭제됐어요)</p>
      </div>
    )
  }
  if (!result) {
    return (
      <div className={frame}>
        <p className="text-[13px] font-medium text-label-alt">서류를 불러오는 중이에요…</p>
      </div>
    )
  }
  if (!result.data) {
    return (
      <div className={frame}>
        <MaterialIcon name="error" size={28} className="text-label-alt" />
        <p role="alert" className="text-[13px] font-semibold text-terracotta">
          {result.message}
        </p>
        <button
          type="button"
          onClick={() => setReloadKey((value) => value + 1)}
          className="h-8 rounded-lg bg-primary px-3 text-[12px] font-bold text-surface"
        >
          다시 불러오기
        </button>
      </div>
    )
  }

  const file = result.data
  return (
    <div>
      <a
        href={file.url}
        target="_blank"
        rel="noreferrer"
        title="원본 크기로 열기"
        className="block h-[300px] overflow-hidden rounded-xl bg-sunken"
      >
        {file.contentType === 'application/pdf' ? (
          <iframe src={file.url} title="서류 PDF" className="pointer-events-none h-full w-full" />
        ) : (
          <img src={file.url} alt="제출한 서류" className="h-full w-full object-contain" />
        )}
      </a>
      <div className="mt-1.5 flex items-center justify-between text-[12px] text-label-alt">
        <span>클릭하면 원본 크기로 열려요 · 주소는 5분 뒤 만료돼요</span>
        <button
          type="button"
          onClick={() => setReloadKey((value) => value + 1)}
          className="font-bold text-accent hover:underline"
        >
          다시 불러오기
        </button>
      </div>
    </div>
  )
}

/** 검토 대기일 때 — (신입생) 입학 연도 + 반려 사유 + [반려] [승인] */
function ReviewActions({
  verification,
  onProcessed,
}: {
  verification: AdminVerificationDetail
  onProcessed: (updated: AdminVerificationDetail) => void
}) {
  // 서버는 올해·내년만 받는다. 화면을 연 시점의 연도로 고정한다
  const [thisYear] = useState(() => new Date().getFullYear())
  const [admissionYear, setAdmissionYear] = useState(thisYear)
  const [reason, setReason] = useState(REJECT_REASONS[0])
  const [submitting, setSubmitting] = useState<'approve' | 'reject' | null>(null)
  const [error, setError] = useState<string | null>(null)
  const freshman = verification.verificationType === 'FRESHMAN'

  const run = async (action: 'approve' | 'reject') => {
    if (submitting) return
    setSubmitting(action)
    setError(null)
    try {
      onProcessed(
        action === 'approve'
          ? await approveAdminVerification(verification.id, freshman ? admissionYear : undefined)
          : await rejectAdminVerification(verification.id, reason),
      )
    } catch (e) {
      setError(errorMessage(e, '처리하지 못했어요. 잠시 후 다시 시도해 주세요.'))
      setSubmitting(null)
    }
  }

  const selectClass =
    'h-11 w-full rounded-[10px] border border-border bg-surface px-3 text-[14px] text-label outline-none focus:border-primary'

  return (
    <div className="mt-5 flex flex-col gap-3">
      {freshman && (
        <div>
          <label htmlFor="admission-year" className="mb-1.5 block text-[13px] font-bold text-body">
            입학 연도 (승인할 때) — 그해 12월 31일까지 유효
          </label>
          <select
            id="admission-year"
            value={admissionYear}
            onChange={(e) => setAdmissionYear(Number(e.target.value))}
            className={selectClass}
          >
            <option value={thisYear}>{thisYear}년</option>
            <option value={thisYear + 1}>{thisYear + 1}년</option>
          </select>
        </div>
      )}
      <div>
        <label htmlFor="reject-reason" className="mb-1.5 block text-[13px] font-bold text-body">
          반려 사유 (반려할 때만)
        </label>
        <select id="reject-reason" value={reason} onChange={(e) => setReason(e.target.value)} className={selectClass}>
          {REJECT_REASONS.map((option) => (
            <option key={option}>{option}</option>
          ))}
        </select>
      </div>
      {error && (
        <p role="alert" className="text-[13px] font-semibold text-terracotta">
          {error}
        </p>
      )}
      <div className="flex gap-2.5">
        <button
          type="button"
          onClick={() => run('reject')}
          disabled={submitting !== null}
          className="h-12 flex-1 rounded-[10px] border border-border bg-surface text-[15px] font-bold text-terracotta-ink disabled:opacity-50"
        >
          {submitting === 'reject' ? '반려 중…' : '반려'}
        </button>
        <button
          type="button"
          onClick={() => run('approve')}
          disabled={submitting !== null}
          className="h-12 flex-[2] rounded-[10px] bg-primary text-[15px] font-bold text-surface disabled:opacity-50"
        >
          {submitting === 'approve' ? '승인 중…' : '승인'}
        </button>
      </div>
    </div>
  )
}

/** 처리 완료 결과 */
function ReviewResult({ verification }: { verification: AdminVerificationDetail }) {
  const by = [verification.reviewerNickname, verification.reviewedAt && `${monthDay(verification.reviewedAt)} 처리`]
    .filter(Boolean)
    .join(' · ')
  return (
    <div className="mt-5 rounded-xl bg-table-head px-4 py-3.5 text-[14px] text-body">
      {verification.status === 'REJECTED' ? (
        <p>
          <span className="font-bold text-terracotta-ink">반려</span> · {verification.rejectionReason}
        </p>
      ) : (
        <p>
          <span className="font-bold text-primary-dark">{verification.status === 'EXPIRED' ? '만료' : '승인'}</span>
          {verification.expiresAt && ` · ${fullDate(verification.expiresAt)}까지`}
        </p>
      )}
      {by && <p className="mt-1 text-[12px] text-label-alt">{by}</p>}
    </div>
  )
}

type DetailResult = { data: AdminVerificationDetail } | { data: null; message: string }

/** 오른쪽 상세 — 서류를 보고 승인·반려한다 */
function DetailPanel({
  verificationId,
  onProcessed,
}: {
  verificationId: number
  onProcessed: (updated: AdminVerificationDetail) => void
}) {
  const [result, setResult] = useState<DetailResult | null>(null)

  useEffect(() => {
    let ignore = false
    getAdminVerification(verificationId)
      .then((data) => {
        if (!ignore) setResult({ data })
      })
      .catch((e: unknown) => {
        if (!ignore) setResult({ data: null, message: errorMessage(e, '서류 정보를 불러오지 못했어요.') })
      })
    return () => {
      ignore = true
    }
  }, [verificationId])

  if (!result) {
    return (
      <Card className="px-6 py-16 text-center">
        <p className="text-[14px] font-medium text-label-alt">불러오는 중이에요…</p>
      </Card>
    )
  }
  if (!result.data) {
    return (
      <Card className="flex flex-col items-center gap-3 px-6 py-16 text-center">
        <MaterialIcon name="error" size={32} className="text-label-alt" />
        <p role="alert" className="text-[14px] font-bold text-label">
          {result.message}
        </p>
      </Card>
    )
  }

  const verification = result.data
  const handleProcessed = (updated: AdminVerificationDetail) => {
    setResult({ data: updated })
    onProcessed(updated)
  }

  return (
    <Card className="p-6">
      <div className="flex items-center gap-2">
        <h2 className="text-[18px] font-extrabold text-label">{displayName(verification)}</h2>
        <VerificationStatusBadge status={verification.status} />
      </div>
      <p className="mt-1 text-[13px] text-label-alt">
        {TYPE_LABEL[verification.verificationType]} · {DOCUMENT_LABEL[verification.documentType]} ·{' '}
        {monthDay(verification.submittedAt)} 신청 · 닉네임 {verification.nickname} ·{' '}
        {verification.neighborhoodVerified ? '동네 인증 완료' : '동네 인증 안 함'}
      </p>

      <div className="mt-4">
        <DocumentPreview verificationId={verification.id} hasDocument={verification.hasDocument} />
      </div>

      <dl className="mt-4 grid grid-cols-[88px_1fr] gap-y-1.5 text-[13px]">
        <dt className="text-label-alt">확인할 것</dt>
        <dd className="font-bold text-label">{CHECKLIST[verification.verificationType]}</dd>
        <dt className="text-label-alt">서류 보관</dt>
        <dd className="font-bold text-label">검토 후 30일 뒤 자동 삭제</dd>
      </dl>

      {verification.status === 'PENDING' ? (
        <ReviewActions verification={verification} onProcessed={handleProcessed} />
      ) : (
        <ReviewResult verification={verification} />
      )}

      <p className="mt-5 flex items-center gap-2 rounded-xl bg-primary-tint px-4 py-3 text-[12px] font-medium text-primary-dark">
        <MaterialIcon name="visibility_off" size={18} />
        기초수급자 인증 결과는 대기열 가산점에만 쓰이고, 다른 회원에게 보이지 않아요.
      </p>
    </Card>
  )
}

/** 어떤 필터·페이지의 조회 결과인지 함께 들고 있어서, 조건을 바꾸면 이전 결과를 쓰지 않는다 */
type ListResult =
  | { key: string; data: PageResponse<AdminVerificationSummary> }
  | { key: string; data: null; message: string }

/** 인증 서류 (#208) — 왼쪽 목록에서 고르고 오른쪽에서 서류를 보고 승인·반려한다 */
export default function VerificationsPage() {
  const [filter, setFilter] = useState<Filter>('PENDING')
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<ListResult | null>(null)
  /** 직접 고른 신청. 고르지 않았거나 이 페이지에 없으면 목록의 첫 줄을 보여 준다 */
  const [selectedId, setSelectedId] = useState<number | null>(null)

  const key = `${filter}:${page}:${reloadKey}`

  useEffect(() => {
    let ignore = false
    getAdminVerifications(filter === 'ALL' ? undefined : filter, page)
      .then((data) => {
        if (!ignore) setResult({ key, data })
      })
      .catch((e: unknown) => {
        if (!ignore) setResult({ key, data: null, message: errorMessage(e, '서류 목록을 불러오지 못했어요.') })
      })
    return () => {
      ignore = true
    }
  }, [filter, page, key])

  const current = result?.key === key ? result : null

  const handleFilterChange = (next: Filter) => {
    setFilter(next)
    setPage(0)
    setSelectedId(null)
  }

  const columns: Column<AdminVerificationSummary>[] = [
    { key: 'name', header: '신청자', width: '22%', render: (row) => <span className="font-bold">{displayName(row)}</span> },
    {
      key: 'type',
      header: '유형 · 서류',
      render: (row) => `${TYPE_LABEL[row.verificationType]} · ${DOCUMENT_LABEL[row.documentType]}`,
    },
    { key: 'submittedAt', header: '신청일', width: '18%', render: (row) => monthDay(row.submittedAt) },
    { key: 'status', header: '상태', width: '20%', render: (row) => <VerificationStatusBadge status={row.status} /> },
  ]

  let list
  let activeId: number | null = null
  if (!current) {
    list = <p className="px-4 py-12 text-center text-[14px] font-medium text-label-alt">불러오는 중이에요…</p>
  } else if (!current.data) {
    list = (
      <div className="flex flex-col items-center gap-3 px-4 py-12 text-center">
        <p role="alert" className="text-[14px] font-bold text-label">
          {current.message}
        </p>
        <button
          type="button"
          onClick={() => setReloadKey((value) => value + 1)}
          className="h-9 rounded-lg bg-primary px-4 text-[13px] font-bold text-surface"
        >
          다시 시도
        </button>
      </div>
    )
  } else {
    const rows = current.data.content
    activeId = rows.some((row) => row.id === selectedId) ? selectedId : (rows[0]?.id ?? null)
    list = (
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        onRowClick={(row) => setSelectedId(row.id)}
        selectedKey={activeId}
        emptyMessage="해당하는 서류가 없어요"
      />
    )
  }

  const data = current?.data ?? null

  // 처리한 줄의 상태를 바꾸고, 검토 대기 목록이면 다음 대기 건을 고른다
  const handleProcessed = (updated: AdminVerificationDetail) => {
    if (!data) return
    const rows = data.content.map((row) => (row.id === updated.id ? { ...row, status: updated.status } : row))
    setResult({ key, data: { ...data, content: rows } })
    if (filter === 'PENDING') {
      const index = rows.findIndex((row) => row.id === updated.id)
      const next = [...rows.slice(index + 1), ...rows.slice(0, index)].find((row) => row.status === 'PENDING')
      if (next) setSelectedId(next.id)
    }
  }

  return (
    <div className="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)] items-start gap-5">
      <div className="flex flex-col gap-3">
        <Card>
          <div className="px-4 py-3.5">
            <FilterChips options={FILTER_OPTIONS} value={filter} onChange={handleFilterChange} label="상태" />
          </div>
          {list}
        </Card>

        {data && data.totalPages > 1 && (
          <nav aria-label="페이지 이동" className="flex items-center justify-center gap-3">
            <button
              type="button"
              onClick={() => setPage(data.number - 1)}
              disabled={data.first}
              className="flex h-8 items-center gap-0.5 rounded-lg border border-border bg-surface pr-3 pl-1.5 text-[13px] font-bold text-body disabled:opacity-40"
            >
              <MaterialIcon name="chevron_left" size={18} />
              이전
            </button>
            <span className="text-[13px] font-semibold text-label-alt">
              {data.number + 1} / {data.totalPages}
            </span>
            <button
              type="button"
              onClick={() => setPage(data.number + 1)}
              disabled={data.last}
              className="flex h-8 items-center gap-0.5 rounded-lg border border-border bg-surface pr-1.5 pl-3 text-[13px] font-bold text-body disabled:opacity-40"
            >
              다음
              <MaterialIcon name="chevron_right" size={18} />
            </button>
          </nav>
        )}
      </div>

      {/* key: 다른 신청을 고르면 입학 연도·반려 사유 선택과 미리보기를 새로 그린다 */}
      {activeId !== null && <DetailPanel key={activeId} verificationId={activeId} onProcessed={handleProcessed} />}
    </div>
  )
}
