import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getAdminHubTrades, processAdminHubTrade } from '../api/hubTrades'
import Card from '../components/Card'
import DataTable, { type Column } from '../components/DataTable'
import FilterChips from '../components/FilterChips'
import MaterialIcon from '../components/MaterialIcon'
import StatusBadge, { type StatusTone } from '../components/StatusBadge'
import type { AdminHubTrade, HubTradeAction, ReservationStatus } from '../types/hubTrade'
import type { PageResponse } from '../types/page'

type Filter = 'OPEN' | 'DONE' | 'ALL'

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'OPEN', label: '진행 중' },
  { value: 'DONE', label: '끝난 거래' },
  { value: 'ALL', label: '전체' },
]

/** 필터 → GET /admin/hub-trades 의 done 값 */
const DONE_PARAM: Record<Filter, boolean | undefined> = { OPEN: false, DONE: true, ALL: undefined }

const EMPTY_MESSAGE: Record<Filter, string> = {
  OPEN: '진행 중인 거점 거래가 없어요',
  DONE: '끝난 거점 거래가 없어요',
  ALL: '거점 거래가 없어요',
}

/** 회원 앱(frontend/src/lib/reservation.ts)과 같은 문구. SCHEDULED는 직거래 상태라 이 화면에는 나오지 않는다 */
const STATUS: Record<ReservationStatus, { label: string; tone: StatusTone }> = {
  SCHEDULED: { label: '약속 예정', tone: 'waiting' },
  HUB_DROP_SCHEDULED: { label: '거점 입고 예정', tone: 'waiting' },
  HUB_RECEIVED: { label: '거점 보관 중', tone: 'waiting' },
  PICKUP_SCHEDULED: { label: '수령 예정', tone: 'waiting' },
  RECONFIRMED: { label: '수령 확정', tone: 'done' },
  COMPLETED: { label: '전달 완료', tone: 'muted' },
  NO_SHOW: { label: '미수령', tone: 'rejected' },
  CANCELED: { label: '거래 취소', tone: 'muted' },
}

const CLOSED: ReservationStatus[] = ['COMPLETED', 'NO_SHOW', 'CANCELED']

/** 되돌릴 수 없는 처리 — 누르기 전에 한 번 더 묻는다 */
const CONFIRM: Record<Exclude<HubTradeAction, 'receive'>, { title: string; description: string; label: string }> = {
  complete: {
    title: '신청자가 물품을 받아 갔나요?',
    description: '수령 완료로 처리하면 거래가 끝나고 되돌릴 수 없어요.',
    label: '수령 완료',
  },
  'no-show': {
    title: '신청자가 받으러 오지 않았나요?',
    description: '미수령으로 처리하면 다음 대기자에게 넘어가요. 대기자가 없으면 물품이 종료되고, 되돌릴 수 없어요.',
    label: '미수령 처리',
  },
}

const ACTION_FAILED: Record<HubTradeAction, string> = {
  receive: '입고 처리하지 못했어요. 잠시 후 다시 시도해 주세요.',
  complete: '수령 완료로 처리하지 못했어요. 잠시 후 다시 시도해 주세요.',
  'no-show': '미수령으로 처리하지 못했어요. 잠시 후 다시 시도해 주세요.',
}

/** YYYY-MM-DDTHH:mm:ss → "10.08 14:54" */
function formatDateTime(dateTime: string) {
  const [, month, day] = dateTime.slice(0, 10).split('-')
  return `${month}.${day} ${dateTime.slice(11, 16)}`
}

/** 상태 배지 아래 한 줄 — 입고 여부와 신청자의 수령 재확인 */
function progressNote(trade: AdminHubTrade) {
  if (trade.status === 'COMPLETED') return trade.completedAt ? `${formatDateTime(trade.completedAt)} 수령` : null
  if (CLOSED.includes(trade.status)) return null
  const hub = trade.atHub && trade.hubReceivedAt ? `${formatDateTime(trade.hubReceivedAt)} 입고` : '입고 전'
  const reconfirm = trade.reconfirmedAt
    ? '신청자 재확인 완료'
    : trade.reconfirmationDeadline
      ? `재확인 기한 ${formatDateTime(trade.reconfirmationDeadline)}`
      : '재확인 전'
  return `${hub} · ${reconfirm}`
}

/** 어떤 조건의 조회 결과인지 함께 들고 있어서, 필터·페이지를 바꾸면 이전 결과를 쓰지 않는다 */
type Result = { key: string; data: PageResponse<AdminHubTrade> } | { key: string; data: null; message: string }

const BUTTON_CLASS = 'h-8 rounded-lg px-3 text-[13px] font-bold whitespace-nowrap disabled:opacity-50'

/**
 * 거점 거래 (#254) — 거점에 들어온 물품을 입고 처리하고, 신청자가 받아 가면 수령 완료, 오지 않으면 미수령으로 처리한다.
 * 직거래는 등록자가 앱에서 전달 완료하므로 이 화면에 나오지 않는다.
 */
export default function HubTradesPage() {
  const [filter, setFilter] = useState<Filter>('OPEN')
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<Result | null>(null)
  /** 처리 요청 중인 예약 */
  const [pendingId, setPendingId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  /** 확인 팝업을 띄운 처리 */
  const [confirming, setConfirming] = useState<{ trade: AdminHubTrade; action: 'complete' | 'no-show' } | null>(null)

  const key = `${filter}:${page}:${reloadKey}`

  useEffect(() => {
    let ignore = false
    getAdminHubTrades(DONE_PARAM[filter], page)
      .then((data) => {
        if (!ignore) setResult({ key, data })
      })
      .catch((e: unknown) => {
        if (ignore) return
        setResult({
          key,
          data: null,
          message: e instanceof ApiError ? e.message : '거점 거래를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
        })
      })
    return () => {
      ignore = true
    }
  }, [filter, page, key])

  const handleFilterChange = (next: Filter) => {
    setFilter(next)
    setPage(0)
    setActionError(null)
  }

  const handleAction = async (trade: AdminHubTrade, action: HubTradeAction) => {
    setConfirming(null)
    if (pendingId !== null) return
    setPendingId(trade.id)
    setActionError(null)
    try {
      const updated = await processAdminHubTrade(trade.id, action)
      if (action === 'receive') {
        // 입고는 그 줄만 바꾼다 — 이어서 수령 완료·미수령을 누를 수 있게 자리를 지킨다
        setResult((current) =>
          current?.data
            ? { ...current, data: { ...current.data, content: current.data.content.map((row) => (row.id === updated.id ? updated : row)) } }
            : current,
        )
      } else {
        // 수령 완료·미수령은 진행 중 목록에서 빠지고, 미수령은 다음 대기자의 예약이 새로 생기므로 다시 불러온다
        setReloadKey((value) => value + 1)
      }
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : ACTION_FAILED[action])
      // 그 사이 상태가 바뀌었을 수 있어서(신청자 재확인, 자동 승계 등) 목록도 새로 받는다
      if (e instanceof ApiError && e.status === 409) setReloadKey((value) => value + 1)
    } finally {
      setPendingId(null)
    }
  }

  const columns: Column<AdminHubTrade>[] = [
    {
      key: 'item',
      header: '물품',
      render: (trade) => (
        <div className="min-w-0">
          <p className="truncate font-semibold">{trade.itemName}</p>
          <p className="mt-0.5 truncate text-[12px] text-label-alt">
            {trade.ownerNickname} → {trade.applicantNickname}
          </p>
        </div>
      ),
    },
    {
      key: 'status',
      header: '진행 상태',
      width: '270px',
      render: (trade) => {
        const note = progressNote(trade)
        return (
          <div className="min-w-0">
            <StatusBadge tone={STATUS[trade.status].tone}>{STATUS[trade.status].label}</StatusBadge>
            {note && <p className="mt-1 truncate text-[12px] text-label-alt">{note}</p>}
          </div>
        )
      },
    },
    {
      key: 'assignedAt',
      header: '배정',
      width: '110px',
      render: (trade) => <span className="text-[13px] text-body">{formatDateTime(trade.assignedAt)}</span>,
    },
    {
      key: 'action',
      header: '',
      width: '200px',
      render: (trade) => {
        if (CLOSED.includes(trade.status)) return null
        const disabled = pendingId !== null
        return trade.atHub ? (
          <div className="flex gap-1.5">
            <button
              type="button"
              onClick={() => setConfirming({ trade, action: 'complete' })}
              disabled={disabled}
              className={`${BUTTON_CLASS} bg-primary text-surface`}
            >
              수령 완료
            </button>
            <button
              type="button"
              onClick={() => setConfirming({ trade, action: 'no-show' })}
              disabled={disabled}
              className={`${BUTTON_CLASS} border border-border bg-surface text-terracotta-ink`}
            >
              미수령
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => handleAction(trade, 'receive')}
            disabled={disabled}
            className={`${BUTTON_CLASS} border border-border bg-surface text-primary-dark`}
          >
            {pendingId === trade.id ? '처리 중…' : '입고 처리'}
          </button>
        )
      },
    },
  ]

  const current = result?.key === key ? result : null
  const data = current?.data ?? null
  const loadError = current && current.data === null ? current.message : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <FilterChips options={FILTERS} value={filter} onChange={handleFilterChange} label="거래 범위" />
        {data && <p className="text-[13px] text-label-alt">총 {data.totalElements.toLocaleString()}건</p>}
      </div>

      {actionError && (
        <p role="alert" className="text-[13px] font-semibold text-terracotta">
          {actionError}
        </p>
      )}

      {!current ? (
        <Card className="px-6 py-16 text-center">
          <p className="text-[14px] font-medium text-label-alt">거점 거래를 불러오는 중이에요…</p>
        </Card>
      ) : !data ? (
        <Card className="flex flex-col items-center gap-3 px-6 py-16 text-center">
          <MaterialIcon name="error" size={32} className="text-label-alt" />
          <p role="alert" className="text-[14px] font-bold text-label">
            {loadError}
          </p>
          <button
            type="button"
            onClick={() => setReloadKey((value) => value + 1)}
            className="h-9 rounded-lg bg-primary px-4 text-[13px] font-bold text-surface"
          >
            다시 시도
          </button>
        </Card>
      ) : (
        <>
          <Card>
            <DataTable
              columns={columns}
              rows={data.content}
              rowKey={(trade) => trade.id}
              emptyMessage={EMPTY_MESSAGE[filter]}
              dimmed={(trade) => CLOSED.includes(trade.status)}
            />
          </Card>

          {data.totalPages > 1 && (
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
        </>
      )}

      {confirming && (
        <div className="fixed inset-0 z-10 flex items-center justify-center bg-label/40 px-6">
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="hub-trade-confirm-title"
            className="w-[380px] rounded-2xl bg-surface p-6"
          >
            <h2 id="hub-trade-confirm-title" className="text-[16px] font-extrabold text-label">
              {CONFIRM[confirming.action].title}
            </h2>
            <p className="mt-1.5 truncate text-[13px] font-semibold text-body">
              {confirming.trade.itemName} · {confirming.trade.applicantNickname}
            </p>
            <p className="mt-2 text-[13px] leading-[1.6] text-label-alt">{CONFIRM[confirming.action].description}</p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirming(null)}
                className="h-10 rounded-[10px] border border-border bg-surface px-4 text-[14px] font-bold text-body"
              >
                취소
              </button>
              <button
                type="button"
                onClick={() => handleAction(confirming.trade, confirming.action)}
                className={`h-10 rounded-[10px] px-4 text-[14px] font-bold text-surface ${
                  confirming.action === 'complete' ? 'bg-primary' : 'bg-terracotta'
                }`}
              >
                {CONFIRM[confirming.action].label}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
