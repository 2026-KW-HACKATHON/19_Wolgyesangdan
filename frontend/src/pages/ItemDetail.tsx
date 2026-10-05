import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getItem } from '../api/items'
import Badge from '../components/Badge'
import ItemThumb from '../components/ItemThumb'
import Toast from '../components/Toast'
import { ITEM_STATUS_LABEL, TRADE_METHOD_LABEL, formatDeadline, itemStatusTone } from '../lib/item'
import type { ItemDetail as ItemDetailData } from '../types/item'

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

  const [showCopyToast, setShowCopyToast] = useState(false)
  const copyTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (copyTimeoutRef.current) clearTimeout(copyTimeoutRef.current)
    }
  }, [])

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
  }, [id])

  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      setShowCopyToast(true)
      if (copyTimeoutRef.current) clearTimeout(copyTimeoutRef.current)
      copyTimeoutRef.current = setTimeout(() => setShowCopyToast(false), 2000)
    } catch {
      // 클립보드 접근 권한이 없는 등 - 조용히 무시
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

      <Toast visible={showCopyToast}>URL을 복사했어요</Toast>
    </div>
  )
}
