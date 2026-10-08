import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { answerAdminInquiry, getAdminInquiries, getAdminInquiry } from '../api/inquiries'
import Card from '../components/Card'
import MaterialIcon from '../components/MaterialIcon'
import StatusBadge from '../components/StatusBadge'
import type { AdminInquiryDetail, AdminInquirySummary, InquiryCategory, InquiryStatus } from '../types/inquiry'
import type { PageResponse } from '../types/page'

const CATEGORY_LABEL: Record<InquiryCategory, string> = {
  TRADE: '거래',
  VERIFICATION: '인증',
  HUB: '거점',
  ETC: '기타',
}

const STATUS_LABEL: Record<InquiryStatus, string> = {
  OPEN: '답변 대기',
  ANSWERED: '답변 완료',
}

// 서버 검증과 같은 길이 (InquiryAnswerRequest)
const ANSWER_MAX = 2000

/** YYYY-MM-DDTHH:mm:ss → "2026.10.08" */
function formatDate(dateTime: string) {
  return dateTime.slice(0, 10).split('-').join('.')
}

function CategoryChip({ category }: { category: InquiryCategory }) {
  return (
    <span className="inline-flex flex-none items-center rounded-full border border-border bg-surface px-2 py-[2px] text-[11px] font-bold text-body">
      {CATEGORY_LABEL[category]}
    </span>
  )
}

function InquiryStatusBadge({ status }: { status: InquiryStatus }) {
  return <StatusBadge tone={status === 'OPEN' ? 'waiting' : 'done'}>{STATUS_LABEL[status]}</StatusBadge>
}

interface DetailPanelProps {
  inquiryId: number
  /** 답변을 보낸 뒤 — 목록의 그 줄 상태를 바꾼다 */
  onAnswered: (inquiry: AdminInquiryDetail) => void
}

type DetailResult = { id: number; data: AdminInquiryDetail } | { id: number; data: null; message: string }

/** 오른쪽 상세 — 본문을 보고 답변한다. 답변은 한 번만 보낼 수 있다 (수정·삭제 없음) */
function DetailPanel({ inquiryId, onAnswered }: DetailPanelProps) {
  const [result, setResult] = useState<DetailResult | null>(null)
  const [answer, setAnswer] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    getAdminInquiry(inquiryId)
      .then((data) => {
        if (!ignore) setResult({ id: inquiryId, data })
      })
      .catch((e: unknown) => {
        if (ignore) return
        setResult({
          id: inquiryId,
          data: null,
          message: e instanceof ApiError ? e.message : '문의를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
        })
      })
    return () => {
      ignore = true
    }
  }, [inquiryId])

  const current = result?.id === inquiryId ? result : null

  if (!current) {
    return (
      <Card className="px-6 py-16 text-center">
        <p className="text-[14px] font-medium text-label-alt">문의를 불러오는 중이에요…</p>
      </Card>
    )
  }

  if (!current.data) {
    return (
      <Card className="flex flex-col items-center gap-3 px-6 py-16 text-center">
        <MaterialIcon name="error" size={32} className="text-label-alt" />
        <p role="alert" className="text-[14px] font-bold text-label">
          {current.message}
        </p>
      </Card>
    )
  }

  const inquiry = current.data

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!answer.trim() || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const answered = await answerAdminInquiry(inquiry.id, answer.trim())
      setResult({ id: answered.id, data: answered })
      onAnswered(answered)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '답변을 보내지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card className="p-6">
      <div className="flex items-center gap-2">
        <CategoryChip category={inquiry.category} />
        <InquiryStatusBadge status={inquiry.status} />
      </div>
      <h2 className="mt-2.5 text-[17px] leading-[1.4] font-extrabold break-words text-label">{inquiry.title}</h2>
      <p className="mt-1 text-[13px] text-label-alt">
        {inquiry.nickname} · {formatDate(inquiry.createdAt)}
      </p>

      <div className="mt-4 rounded-xl bg-screen px-4 py-3.5 text-[14px] leading-[1.65] break-words whitespace-pre-wrap text-body">
        {inquiry.content}
      </div>

      {inquiry.status === 'ANSWERED' ? (
        <div className="mt-5 border-l-[3px] border-primary pl-4">
          <p className="text-[13px] font-bold text-primary-dark">
            운영자 답변
            {inquiry.answeredAt && (
              <span className="ml-2 font-medium text-label-alt">{formatDate(inquiry.answeredAt)}</span>
            )}
          </p>
          <p className="mt-1.5 text-[14px] leading-[1.65] break-words whitespace-pre-wrap text-body">{inquiry.answer}</p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-5">
          <label htmlFor="inquiry-answer" className="mb-1.5 block text-[13px] font-bold text-body">
            답변
          </label>
          <textarea
            id="inquiry-answer"
            value={answer}
            onChange={(e) => {
              setAnswer(e.target.value)
              setError(null)
            }}
            maxLength={ANSWER_MAX}
            placeholder="회원이 앱에서 확인할 답변을 적어 주세요. 보낸 뒤에는 고칠 수 없어요."
            className="h-[120px] w-full resize-none rounded-[10px] border border-border bg-surface px-3.5 py-2.5 text-[14px] leading-[1.6] text-label outline-none focus:border-primary"
          />
          {error && (
            <p role="alert" className="mt-2 text-[13px] font-semibold text-terracotta">
              {error}
            </p>
          )}
          <div className="mt-3 flex justify-end">
            <button
              type="submit"
              disabled={!answer.trim() || submitting}
              className="h-10 rounded-[10px] bg-primary px-4 text-[14px] font-bold text-surface disabled:opacity-50"
            >
              {submitting ? '보내는 중…' : '답변 보내기'}
            </button>
          </div>
        </form>
      )}
    </Card>
  )
}

/** 어떤 페이지의 조회 결과인지 함께 들고 있어서, 페이지를 바꾸면 이전 결과를 쓰지 않는다 */
type ListResult =
  | { key: string; data: PageResponse<AdminInquirySummary> }
  | { key: string; data: null; message: string }

/** 문의 (#213) — 왼쪽 목록에서 고르고 오른쪽 상세에서 답변한다 */
export default function InquiriesPage() {
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<ListResult | null>(null)
  /** 직접 고른 문의. 고르지 않았으면 목록의 첫 문의를 보여 준다 */
  const [selectedId, setSelectedId] = useState<number | null>(null)

  const key = `${page}:${reloadKey}`

  useEffect(() => {
    let ignore = false
    getAdminInquiries(undefined, page)
      .then((data) => {
        if (!ignore) setResult({ key, data })
      })
      .catch((e: unknown) => {
        if (ignore) return
        setResult({
          key,
          data: null,
          message: e instanceof ApiError ? e.message : '문의를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
        })
      })
    return () => {
      ignore = true
    }
  }, [page, key])

  const current = result?.key === key ? result : null

  if (!current) {
    return (
      <Card className="px-6 py-16 text-center">
        <p className="text-[14px] font-medium text-label-alt">문의를 불러오는 중이에요…</p>
      </Card>
    )
  }

  if (!current.data) {
    return (
      <Card className="flex flex-col items-center gap-3 px-6 py-16 text-center">
        <MaterialIcon name="error" size={32} className="text-label-alt" />
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
      </Card>
    )
  }

  const data = current.data

  if (data.content.length === 0) {
    return (
      <Card className="flex flex-col items-center gap-2 px-6 py-16 text-center">
        <MaterialIcon name="forum" size={32} className="text-label-alt" />
        <p className="text-[15px] font-bold text-label">들어온 문의가 없어요</p>
      </Card>
    )
  }

  // 고른 문의가 이 페이지에 없으면(처음 들어왔거나 페이지를 넘긴 경우) 첫 문의를 보여 준다
  const activeId = data.content.some((inquiry) => inquiry.id === selectedId) ? selectedId! : data.content[0].id

  const handleAnswered = (answered: AdminInquiryDetail) => {
    // 그 줄의 상태만 바꾼다 — 순서를 바로 바꾸면 방금 답변한 문의가 목록에서 자리를 옮겨 헷갈린다
    setResult((value) =>
      value?.data
        ? {
            ...value,
            data: {
              ...value.data,
              content: value.data.content.map((row) => (row.id === answered.id ? { ...row, status: answered.status } : row)),
            },
          }
        : value,
    )
  }

  const handlePageChange = (next: number) => {
    setPage(next)
    setSelectedId(null)
  }

  return (
    <div className="grid grid-cols-[380px_minmax(0,1fr)] items-start gap-5">
      <div className="flex flex-col gap-3">
        <Card>
          <ul>
            {data.content.map((inquiry, index) => (
              <li key={inquiry.id} className={index > 0 ? 'border-t border-row-line' : ''}>
                <button
                  type="button"
                  onClick={() => setSelectedId(inquiry.id)}
                  aria-current={inquiry.id === activeId}
                  className={`block w-full px-4 py-3.5 text-left hover:bg-row-selected ${
                    inquiry.id === activeId ? 'bg-row-selected' : ''
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <CategoryChip category={inquiry.category} />
                    <span className="min-w-0 flex-1 truncate text-[14px] font-bold text-label">{inquiry.title}</span>
                    <InquiryStatusBadge status={inquiry.status} />
                  </div>
                  <p className="mt-1.5 text-[12px] text-label-alt">
                    {inquiry.nickname} · {formatDate(inquiry.createdAt)}
                  </p>
                </button>
              </li>
            ))}
          </ul>
        </Card>

        {data.totalPages > 1 && (
          <nav aria-label="페이지 이동" className="flex items-center justify-center gap-3">
            <button
              type="button"
              onClick={() => handlePageChange(data.number - 1)}
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
              onClick={() => handlePageChange(data.number + 1)}
              disabled={data.last}
              className="flex h-8 items-center gap-0.5 rounded-lg border border-border bg-surface pr-1.5 pl-3 text-[13px] font-bold text-body disabled:opacity-40"
            >
              다음
              <MaterialIcon name="chevron_right" size={18} />
            </button>
          </nav>
        )}
      </div>

      {/* key: 다른 문의를 고르면 쓰던 답변이 넘어가지 않게 새로 그린다 */}
      <DetailPanel key={activeId} inquiryId={activeId} onAnswered={handleAnswered} />
    </div>
  )
}
