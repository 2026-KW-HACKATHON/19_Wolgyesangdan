import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getItem } from '../api/items'
import { isItemModifiable } from '../lib/item'
import type { ItemDetail } from '../types/item'
import ItemRegisterPage from './ItemRegisterPage'

/** 어떤 id의 조회 결과인지 함께 들고 있어서, 다른 물품으로 넘어가면 이전 결과를 쓰지 않는다 */
type Result = { id: string; item: ItemDetail } | { id: string; item: null; message: string }

/**
 * 물품 수정 (/items/:id/edit). 상세를 불러와 등록 화면을 수정 모드로 띄운다.
 * 내 물품이고 신청을 받는 중에 신청자가 0명일 때만 — 아니면 안내 문구만 보여준다 (서버도 같은 기준으로 거절한다)
 */
export default function ItemEditPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    let ignore = false
    const request = /^\d+$/.test(id)
      ? getItem(id)
      : Promise.reject(new ApiError(404, 'ITEM_NOT_FOUND', '존재하지 않는 물품입니다.'))
    request
      .then((item) => {
        if (!ignore) setResult({ id, item })
      })
      .catch((e: unknown) => {
        if (ignore) return
        const notFound = e instanceof ApiError && (e.status === 404 || e.status === 400)
        setResult({
          id,
          item: null,
          message: notFound ? '물품을 찾을 수 없어요' : '물품을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
        })
      })
    return () => {
      ignore = true
    }
  }, [id])

  const current = result?.id === id ? result : null

  if (!current) {
    return (
      <div className="flex h-full items-center justify-center px-6 text-center">
        <p className="text-sm font-medium text-[var(--color-label-alt)]">물품을 불러오는 중이에요…</p>
      </div>
    )
  }

  const item = current.item
  const blockedMessage = !item
    ? current.message
    : !item.isMine
      ? '내가 등록한 물품만 수정할 수 있어요'
      : !isItemModifiable(item)
        ? '신청자가 있거나 신청을 받는 중이 아니라서 수정할 수 없어요'
        : null

  if (!item || blockedMessage) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
        <span className="ms text-4xl text-[var(--color-label-alt)]">edit_off</span>
        <p className="text-base font-bold text-[var(--color-label)]">{blockedMessage}</p>
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-bold text-[var(--color-surface)]"
        >
          돌아가기
        </button>
      </div>
    )
  }

  // 바텀 탭이 없는 화면이라 등록 화면(AppLayout의 main)처럼 스크롤 영역을 직접 둔다
  return (
    <main className="flex-1 overflow-y-auto">
      <ItemRegisterPage key={item.id} editing={item} />
    </main>
  )
}
