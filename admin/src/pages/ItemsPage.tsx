import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { closeAdminItemApplications, getAdminItems, setAdminItemHidden } from '../api/items'
import Card from '../components/Card'
import DataTable, { type Column } from '../components/DataTable'
import FilterChips from '../components/FilterChips'
import MaterialIcon from '../components/MaterialIcon'
import StatusBadge, { type StatusTone } from '../components/StatusBadge'
import type { AdminItem, ItemStatus } from '../types/item'
import type { PageResponse } from '../types/page'

type Filter = 'ALL' | 'HIDDEN'

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'HIDDEN', label: '숨긴 물품' },
]

/** 회원 앱(frontend/src/lib/item.ts)과 같은 문구 */
const STATUS: Record<ItemStatus, { label: string; tone: StatusTone }> = {
  REGISTERED: { label: '등록됨', tone: 'muted' },
  OPEN: { label: '신청 가능', tone: 'done' },
  CLOSED: { label: '배정 중', tone: 'waiting' },
  ASSIGNED: { label: '배정 완료', tone: 'waiting' },
  COMPLETED: { label: '거래 완료', tone: 'muted' },
  CANCELED: { label: '취소됨', tone: 'muted' },
}

/** 신청 조기 마감을 할 수 있는 상태 — 신청 받는 중이거나 정원이 차서 배정을 기다리는 중 */
function canCloseApplications(item: AdminItem) {
  return item.status === 'OPEN' || item.status === 'CLOSED'
}

/** YYYY-MM-DDTHH:mm:ss → "2026.10.08" */
function formatDate(dateTime: string) {
  return dateTime.slice(0, 10).split('-').join('.')
}

/** 어떤 조건의 조회 결과인지 함께 들고 있어서, 필터·페이지를 바꾸면 이전 결과를 쓰지 않는다 */
type Result = { key: string; data: PageResponse<AdminItem> } | { key: string; data: null; message: string }

/**
 * 물품 관리 (#215) — 등록된 물품을 보고 숨기거나 다시 보이게 한다. 영구 삭제는 없다.
 * 신청 받는 중인 물품은 지금 마감하고 바로 1순위에게 배정할 수 있다 (#275).
 */
export default function ItemsPage() {
  const [filter, setFilter] = useState<Filter>('ALL')
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<Result | null>(null)
  /** 숨기기/다시 보이기 요청 중인 물품 */
  const [pendingId, setPendingId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionNotice, setActionNotice] = useState<string | null>(null)
  /** 조기 마감 확인 팝업을 띄운 물품 */
  const [closing, setClosing] = useState<AdminItem | null>(null)

  const key = `${filter}:${page}:${reloadKey}`

  useEffect(() => {
    let ignore = false
    getAdminItems(filter === 'HIDDEN' ? true : undefined, page)
      .then((data) => {
        if (!ignore) setResult({ key, data })
      })
      .catch((e: unknown) => {
        if (ignore) return
        setResult({
          key,
          data: null,
          message: e instanceof ApiError ? e.message : '물품을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
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
    setActionNotice(null)
  }

  /** 목록에서 그 줄만 바꾼다 */
  const replaceRow = (updated: AdminItem) => {
    setResult((current) =>
      current?.data
        ? { ...current, data: { ...current.data, content: current.data.content.map((row) => (row.id === updated.id ? updated : row)) } }
        : current,
    )
  }

  const handleToggle = async (item: AdminItem) => {
    if (pendingId !== null) return
    setPendingId(item.id)
    setActionError(null)
    setActionNotice(null)
    try {
      // 그 줄만 바꾼다 — "숨긴 물품" 필터에서 다시 보이게 해도 바로 사라지지 않아서 잘못 눌렀을 때 되돌릴 수 있다
      replaceRow(await setAdminItemHidden(item.id, !item.hidden))
    } catch (e) {
      setActionError(
        e instanceof ApiError
          ? e.message
          : `${item.hidden ? '다시 보이게 하지' : '숨기지'} 못했어요. 잠시 후 다시 시도해 주세요.`,
      )
    } finally {
      setPendingId(null)
    }
  }

  const handleCloseApplications = async (item: AdminItem) => {
    setClosing(null)
    if (pendingId !== null) return
    setPendingId(item.id)
    setActionError(null)
    setActionNotice(null)
    try {
      const updated = await closeAdminItemApplications(item.id)
      replaceRow(updated)
      setActionNotice(
        updated.status === 'ASSIGNED'
          ? `'${updated.name}' 신청을 마감하고 1순위 신청자에게 배정했어요.`
          : `'${updated.name}'에 신청자가 없어 물품을 종료했어요.`,
      )
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : '신청을 마감하지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setPendingId(null)
    }
  }

  const columns: Column<AdminItem>[] = [
    {
      key: 'item',
      header: '물품',
      render: (item) => (
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="truncate font-semibold">{item.name}</span>
            {item.hidden && <StatusBadge tone="muted">숨김</StatusBadge>}
          </div>
          <p className="mt-0.5 truncate text-[12px] text-label-alt">
            {item.ownerNickname} · {item.categoryGroup}
          </p>
        </div>
      ),
    },
    {
      key: 'status',
      header: '상태',
      width: '140px',
      render: (item) => <StatusBadge tone={STATUS[item.status].tone}>{STATUS[item.status].label}</StatusBadge>,
    },
    {
      key: 'createdAt',
      header: '등록일',
      width: '140px',
      render: (item) => <span className="text-[13px] text-body">{formatDate(item.createdAt)}</span>,
    },
    {
      key: 'action',
      header: '',
      width: '240px',
      render: (item) => (
        <div className="flex justify-end gap-2">
          {canCloseApplications(item) && (
            <button
              type="button"
              onClick={() => setClosing(item)}
              disabled={pendingId !== null}
              className="h-8 rounded-lg bg-primary px-3 text-[13px] font-bold whitespace-nowrap text-surface disabled:opacity-50"
            >
              마감하고 배정
            </button>
          )}
          <button
            type="button"
            onClick={() => handleToggle(item)}
            disabled={pendingId !== null}
            className={`h-8 rounded-lg border border-border bg-surface px-3 text-[13px] font-bold whitespace-nowrap disabled:opacity-50 ${
              item.hidden ? 'text-primary-dark' : 'text-body'
            }`}
          >
            {item.hidden ? '다시 보이기' : '숨기기'}
          </button>
        </div>
      ),
    },
  ]

  const current = result?.key === key ? result : null
  // 숨기기/다시 보이기로 그 줄만 바꾼 결과는 key가 같아서 그대로 보인다
  const data = current?.data ?? null
  const loadError = current && current.data === null ? current.message : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <FilterChips options={FILTERS} value={filter} onChange={handleFilterChange} label="물품 범위" />
        {data && <p className="text-[13px] text-label-alt">총 {data.totalElements.toLocaleString()}개</p>}
      </div>

      {actionNotice && (
        <p role="status" className="text-[13px] font-semibold text-primary-dark">
          {actionNotice}
        </p>
      )}

      {actionError && (
        <p role="alert" className="text-[13px] font-semibold text-terracotta">
          {actionError}
        </p>
      )}

      {!current ? (
        <Card className="px-6 py-16 text-center">
          <p className="text-[14px] font-medium text-label-alt">물품을 불러오는 중이에요…</p>
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
              rowKey={(item) => item.id}
              emptyMessage={filter === 'HIDDEN' ? '숨긴 물품이 없어요' : '등록된 물품이 없어요'}
              dimmed={(item) => item.hidden}
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

      {closing && (
        <div className="fixed inset-0 z-10 flex items-center justify-center bg-label/40 px-6">
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="close-applications-title"
            className="w-[380px] rounded-2xl bg-surface p-6"
          >
            <h2 id="close-applications-title" className="text-[16px] font-extrabold text-label">
              신청을 지금 마감하고 배정할까요?
            </h2>
            <p className="mt-1.5 truncate text-[13px] font-semibold text-body">
              {closing.name} · {closing.ownerNickname}
            </p>
            <p className="mt-2 text-[13px] leading-[1.6] text-label-alt">
              신청 마감을 지금으로 당기고, 우선배정 점수와 신청 순서에 따라 1순위 신청자에게 바로 배정해요. 신청자가
              없으면 물품이 종료돼요. 되돌릴 수 없어요.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setClosing(null)}
                className="h-10 rounded-[10px] border border-border bg-surface px-4 text-[14px] font-bold text-body"
              >
                취소
              </button>
              <button
                type="button"
                onClick={() => handleCloseApplications(closing)}
                className="h-10 rounded-[10px] bg-primary px-4 text-[14px] font-bold text-surface"
              >
                마감하고 배정
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
