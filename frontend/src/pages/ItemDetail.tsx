import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { applyForItem } from '../api/applications'
import { ApiError } from '../api/client'
import { getItem } from '../api/items'
import Badge from '../components/Badge'
import ItemThumb from '../components/ItemThumb'
import Toast from '../components/Toast'
import { isLoggedIn } from '../lib/authStorage'
import { loginPath } from '../lib/loginRedirect'
import { ITEM_STATUS_LABEL, TRADE_METHOD_LABEL, formatDeadline, itemStatusTone } from '../lib/item'
import type { ItemDetail as ItemDetailData, ItemStatus } from '../types/item'

/** 신청할 수 없는 상태일 때 신청 버튼 자리에 보여주는 문구 */
const CLOSED_BUTTON_LABEL: Partial<Record<ItemStatus, string>> = {
  CLOSED: '신청이 마감됐어요',
  ASSIGNED: '배정이 끝났어요',
  COMPLETED: '거래가 끝났어요',
}

/** 이미 신청한 사람에게 보여줄 버튼 문구 — 누르면 마이페이지 "내가 신청한 물품"으로 간다 */
function myApplicationLabel({ status, waitlistRank }: NonNullable<ItemDetailData['myApplication']>) {
  if (status === 'SELECTED') return '나에게 배정됐어요 · 내 신청 보기'
  if (status === 'COMPLETED') return '받은 물건이에요 · 내 신청 보기'
  return waitlistRank ? `신청 완료 · 대기 ${waitlistRank}번` : '신청 완료 · 내 신청 보기'
}

/** 신청이 막힌 사유 중 사용자가 바로 해결할 수 있는 것 — 해결하러 가는 버튼을 함께 보여준다 */
const APPLY_ERROR_ACTION: Record<string, { label: string; to: (itemId: string) => string }> = {
  APPLICATION_NOT_ELIGIBLE: { label: '동네 인증하러 가기', to: () => '/verify/location' },
  APPLICATION_CONTACT_NOT_SET: {
    label: '연락 수단 설정하러 가기',
    to: (itemId) => `/settings/contact?next=/items/${itemId}`,
  },
}

function SpecRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-3 py-1.5">
      <span className="w-20 flex-none text-sm font-medium text-[var(--color-label-alt)]">{label}</span>
      <span className="flex-1 text-sm font-semibold text-[var(--color-label)]">{value}</span>
    </div>
  )
}

/** "2026-10-06" → "10.6" */
function formatMonthDay(date: string) {
  const [, month, day] = date.split('-')
  return `${Number(month)}.${Number(day)}`
}

/** 전달 가능 기간. 한쪽만 있으면 "10.6부터" / "10.20까지" */
function formatAvailablePeriod(from: string | null, until: string | null) {
  if (from && until) return `${formatMonthDay(from)} ~ ${formatMonthDay(until)}`
  if (from) return `${formatMonthDay(from)}부터`
  if (until) return `${formatMonthDay(until)}까지`
  return null
}

/** 어떤 id의 조회 결과인지 함께 들고 있어서, 다른 물품으로 넘어가면 이전 결과를 쓰지 않는다 */
type Result =
  | { id: string; item: ItemDetailData }
  | { id: string; item: null; notFound: boolean; message: string }

export default function ItemDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const [result, setResult] = useState<Result | null>(null)
  // 신청에 성공하면 대기 인원이 바로 보이도록 상세를 다시 받는다
  const [reloadKey, setReloadKey] = useState(0)

  const [applying, setApplying] = useState(false)
  // 이 화면에서 신청을 마친 물품 / 신청이 막힌 사유. 다른 물품으로 넘어가면 쓰지 않도록 id와 함께 둔다
  const [appliedId, setAppliedId] = useState<string | null>(null)
  const [applyError, setApplyError] = useState<{ id: string; code: string; message: string } | null>(null)

  const [toast, setToast] = useState({ visible: false, message: '' })
  const toastTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current)
    }
  }, [])

  const showToast = (message: string) => {
    setToast({ visible: true, message })
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current)
    toastTimeoutRef.current = setTimeout(() => setToast((t) => ({ ...t, visible: false })), 2000)
  }

  // 물품 상세 (GET /items/{itemId}, 비회원 허용)
  useEffect(() => {
    let ignore = false
    // id가 숫자가 아니면 조회하지 않고 없는 물품으로 본다 (/items/categories 같은 다른 API 경로와 겹치지 않게)
    const request = /^\d+$/.test(id)
      ? getItem(id)
      : Promise.reject(new ApiError(404, 'ITEM_NOT_FOUND', '존재하지 않는 물품입니다.'))
    request
      .then((item) => {
        if (!ignore) setResult({ id, item })
      })
      .catch((e: unknown) => {
        if (ignore) return
        // 없는 물품(404)이거나 id가 숫자가 아니면(400) 찾을 수 없는 물품으로 본다
        const notFound = e instanceof ApiError && (e.status === 404 || e.status === 400)
        setResult({
          id,
          item: null,
          notFound,
          message: e instanceof ApiError ? e.message : '물품을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
        })
      })
    return () => {
      ignore = true
    }
  }, [id, reloadKey])

  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      showToast('URL을 복사했어요')
    } catch {
      // 클립보드 접근 권한이 없는 등 - 조용히 무시
    }
  }

  // 물품 신청 (POST /items/{itemId}/applications). 신청은 로그인이 필요하다
  const handleApply = async (itemId: number) => {
    if (applying) return
    if (!isLoggedIn()) {
      navigate(loginPath(`/items/${id}`))
      return
    }
    setApplying(true)
    setApplyError(null)
    try {
      const application = await applyForItem(itemId)
      setAppliedId(id)
      showToast(application.waitlistRank ? `신청했어요 · 대기 ${application.waitlistRank}번` : '신청했어요')
      setReloadKey((key) => key + 1)
    } catch (e) {
      // 로그인이 풀린 경우 — apiFetch가 로그인을 정리하고 "로그인이 필요해요" 팝업(SessionExpiredDialog)을 띄운다
      if (e instanceof ApiError && e.status === 401) return
      setApplyError({
        id,
        code: e instanceof ApiError ? e.code : 'UNKNOWN',
        message: e instanceof ApiError ? e.message : '신청하지 못했어요. 잠시 후 다시 시도해 주세요.',
      })
    } finally {
      setApplying(false)
    }
  }

  const current = result?.id === id ? result : null

  if (!current) {
    return (
      <div className="flex h-full items-center justify-center px-6 text-center">
        <p className="text-sm font-medium text-[var(--color-label-alt)]">물품을 불러오는 중이에요…</p>
      </div>
    )
  }

  if (!current.item) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
        <span className="ms text-4xl text-[var(--color-label-alt)]">{current.notFound ? 'search_off' : 'error'}</span>
        <p className="text-base font-bold text-[var(--color-label)]">
          {current.notFound ? '물품을 찾을 수 없어요' : current.message}
        </p>
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

  const item = current.item
  const images = [...item.images].sort((a, b) => a.displayOrder - b.displayOrder)
  // 세부 카테고리는 선택 입력이라 없으면 뺀다
  const subtitle = [item.categoryGroup, item.category, item.conditionGrade].filter(Boolean).join(' · ')
  const availablePeriod = formatAvailablePeriod(item.availableFrom, item.availableUntil)
  const deadline = formatDeadline(item.applicationDeadline)

  // 방금 이 화면에서 신청했으면 상세를 다시 받는 동안에도 신청 완료로 둔다 (그 사이 다시 누르지 않게)
  const applied = appliedId === id
  const currentApplyError = applyError?.id === id ? applyError : null
  const applyErrorAction = currentApplyError ? APPLY_ERROR_ACTION[currentApplyError.code] : undefined
  const canApply = item.status === 'OPEN' && !applied
  const applyLabel = applied
    ? '신청 완료'
    : applying
      ? '신청 중…'
      : item.status === 'OPEN'
        ? '신청하기'
        : (CLOSED_BUTTON_LABEL[item.status] ?? '신청할 수 없어요')
  // 내 물품이거나 이미 신청했으면 신청 버튼 대신 마이페이지로 가는 버튼 (GET /items/{id}의 isMine·myApplication)
  const viewerAction = item.isMine
    ? { label: '내가 등록한 물품이에요 · 관리하기', to: '/mypage' }
    : item.myApplication
      ? { label: myApplicationLabel(item.myApplication), to: '/mypage?tab=applied' }
      : null

  return (
    <div className="relative flex h-full flex-col">
      <header className="flex h-14 flex-none items-center gap-1.5 px-3.5">
        <button type="button" onClick={() => navigate(-1)} className="flex h-9 w-9 items-center justify-center text-[var(--color-label)]">
          <span className="ms text-2xl">chevron_left</span>
        </button>
        <h1 className="flex-1 truncate text-[17px] font-bold text-[var(--color-label)]">{item.name}</h1>
        <button type="button" onClick={handleShare} className="flex h-9 w-9 items-center justify-center text-[#4A4A40]">
          <span className="ms text-xl">ios_share</span>
        </button>
      </header>

      <div className="flex-1 overflow-y-auto">
        {images.length > 0 ? (
          // 사진이 여러 장이면 좌우로 넘겨 본다
          <div className="relative">
            <div className="flex h-[220px] snap-x snap-mandatory overflow-x-auto [scrollbar-width:none]">
              {images.map((image, i) => (
                <ItemThumb
                  key={image.imageUrl}
                  imageUrl={image.imageUrl}
                  categoryGroup={item.categoryGroup}
                  alt={`${item.name} 사진 ${i + 1}`}
                  className="h-full w-full flex-none snap-center"
                  iconClassName="text-[78px]"
                />
              ))}
            </div>
            {images.length > 1 && (
              <span className="absolute right-3 bottom-3 rounded-full bg-[var(--color-label)]/60 px-2 py-0.5 text-[11px] font-bold text-[var(--color-screen)]">
                사진 {images.length}장
              </span>
            )}
          </div>
        ) : (
          <ItemThumb
            imageUrl={null}
            categoryGroup={item.categoryGroup}
            alt={item.name}
            className="h-[220px] w-full"
            iconClassName="text-[78px]"
          />
        )}

        <div className="flex flex-col gap-4 px-5 pt-4 pb-6">
          <div>
            <div className="mb-2 flex flex-wrap gap-1.5">
              <Badge tone={itemStatusTone(item.status)}>{ITEM_STATUS_LABEL[item.status]}</Badge>
              {item.tradeMethods.map((method) => (
                <Badge key={method}>{TRADE_METHOD_LABEL[method]}</Badge>
              ))}
            </div>
            <h2 className="text-2xl font-bold text-[var(--color-label)]">{item.name}</h2>
            <p className="mt-1 text-sm font-medium text-[var(--color-label-alt)]">{subtitle}</p>
          </div>

          <div className="flex items-center gap-3.5 rounded-2xl bg-[#E7EBD8] px-4.5 py-4">
            <span className="flex h-10 w-10 flex-none items-center justify-center rounded-2xl bg-[var(--color-surface)] text-[var(--color-accent)]">
              <span className="ms text-xl">autorenew</span>
            </span>
            <div>
              <div className="flex items-baseline gap-1">
                <span className="text-[28px] font-extrabold text-[var(--color-primary-dark)]">
                  {item.estimatedCarbonReduction}
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
            {/* 선택 입력 항목은 등록자가 적은 것만 보여준다 */}
            {item.usagePeriod && <SpecRow label="사용 기간" value={item.usagePeriod} />}
            <SpecRow label="하자 여부" value={item.defectYn ? (item.defectDescription ?? '있음') : '없음'} />
            {item.workingStatus && <SpecRow label="작동 여부" value={item.workingStatus} />}
            {item.size && <SpecRow label="크기" value={item.size} />}
            {item.transportDifficulty && <SpecRow label="운반 난이도" value={item.transportDifficulty} />}
          </section>

          {item.description && (
            <p className="text-[15px] leading-relaxed font-medium whitespace-pre-line text-[var(--color-body)]">
              {item.description}
            </p>
          )}

          <div className="h-px bg-[var(--color-border)]" />

          <section>
            <h3 className="mb-1 text-lg font-bold text-[var(--color-label)]">전달 방법</h3>
            {deadline && <SpecRow label="신청 마감" value={deadline} />}
            {availablePeriod && <SpecRow label="전달 가능" value={availablePeriod} />}
            <SpecRow
              label="거래 방식"
              value={item.tradeMethods.map((m) => TRADE_METHOD_LABEL[m]).join(' · ')}
            />
            {/* 거점 정보는 거점 수령을 지원하는 물품에만 내려온다 */}
            {item.campaign && item.tradeMethods.includes('CAMPAIGN') && (
              <div className="mt-2 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3.5">
                <div className="flex items-center gap-1.5 text-sm font-bold text-[var(--color-label)]">
                  <span className="ms text-base text-[var(--color-primary)]">location_on</span>
                  {item.campaign.locationName}
                </div>
                <p className="mt-1 text-xs font-medium text-[var(--color-label-alt)]">
                  {[item.campaign.hubHours, '비대면 수령'].filter(Boolean).join(' · ')}
                </p>
              </div>
            )}
          </section>

          <div className="flex items-center gap-2.5 pt-1">
            <span className="flex h-10 w-10 flex-none items-center justify-center rounded-full bg-[#EBE4D1] text-base font-bold text-[#4A4A40]">
              {item.owner.nickname.slice(0, 1)}
            </span>
            <div className="flex-1">
              <div className="text-sm font-bold text-[var(--color-label)]">{item.owner.nickname}</div>
              <div className="mt-0.5 text-xs font-medium text-[var(--color-label-alt)]">
                전달 완료 {item.owner.givenCount}회
              </div>
            </div>
            <span className="text-[13px] font-medium text-[var(--color-label-alt)]">
              대기 {item.applicantCount} / {item.maxApplicants}명
            </span>
          </div>
        </div>
      </div>

      <div className="flex-none border-t border-[var(--color-border)] bg-[var(--color-surface)] px-5 py-3">
        {currentApplyError && (
          <div role="alert" className="mb-2.5 flex items-center gap-2">
            <p className="flex-1 text-[13px] font-semibold text-terracotta">{currentApplyError.message}</p>
            {applyErrorAction && (
              <button
                type="button"
                onClick={() => navigate(applyErrorAction.to(id))}
                className="flex-none text-[13px] font-bold text-[var(--color-accent)] underline underline-offset-[3px]"
              >
                {applyErrorAction.label}
              </button>
            )}
          </div>
        )}
        {viewerAction ? (
          <button
            type="button"
            onClick={() => navigate(viewerAction.to)}
            className="flex h-12 w-full items-center justify-center gap-0.5 rounded-2xl border border-[var(--color-primary)] bg-[var(--color-surface)] text-base font-bold text-[var(--color-accent)]"
          >
            {viewerAction.label}
            <span className="ms text-lg">chevron_right</span>
          </button>
        ) : (
          <button
            type="button"
            onClick={() => handleApply(item.id)}
            disabled={!canApply || applying}
            className="h-12 w-full rounded-2xl bg-[var(--color-primary)] text-base font-bold text-[var(--color-surface)] disabled:opacity-50"
          >
            {applyLabel}
          </button>
        )}
      </div>

      <Toast visible={toast.visible}>{toast.message}</Toast>
    </div>
  )
}
