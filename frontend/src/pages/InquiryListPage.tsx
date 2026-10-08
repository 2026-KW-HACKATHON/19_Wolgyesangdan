import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getMyInquiries } from '../api/inquiries'
import BottomActionBar from '../components/BottomActionBar'
import EmptyState from '../components/EmptyState'
import MaterialIcon from '../components/icons/MaterialIcon'
import PrimaryButton from '../components/PrimaryButton'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import {
  formatInquiryDate,
  INQUIRY_CATEGORY_LABEL,
  INQUIRY_STATUS_BADGE,
  INQUIRY_STATUS_LABEL,
} from '../lib/inquiry'
import type { Inquiry } from '../types/inquiry'

/** 내 문의 목록 (/mypage/inquiries, 로그인 필요). 제목 · 날짜 · 상태. 누르면 상세로 간다 */
export default function InquiryListPage() {
  const navigate = useNavigate()
  const [inquiries, setInquiries] = useState<Inquiry[] | null>(null)
  /** 다음에 불러올 페이지. 더 없으면 null */
  const [nextPage, setNextPage] = useState<number | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let ignore = false
    getMyInquiries(0)
      .then((page) => {
        if (ignore) return
        setInquiries(page.content)
        setNextPage(page.last ? null : 1)
        setError(null)
      })
      .catch((e: unknown) => {
        if (ignore) return
        // 로그인이 풀린 경우 — apiFetch가 "로그인이 필요해요" 팝업(SessionExpiredDialog)을 띄운다
        if (e instanceof ApiError && e.status === 401) return
        setError(e instanceof ApiError ? e.message : '문의를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
      })
    return () => {
      ignore = true
    }
  }, [reloadKey])

  const handleLoadMore = async () => {
    if (nextPage === null || loadingMore) return
    setLoadingMore(true)
    try {
      const page = await getMyInquiries(nextPage)
      setInquiries((current) => [...(current ?? []), ...page.content])
      setNextPage(page.last ? null : nextPage + 1)
    } catch {
      // 더 보기 실패는 버튼을 그대로 둬서 다시 누를 수 있게 한다
    } finally {
      setLoadingMore(false)
    }
  }

  return (
    <Screen>
      <TopBar title="문의하기" onBack={() => navigate('/mypage')} />

      {error ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
          <MaterialIcon name="error" size={36} className="text-label-alt" />
          <p role="alert" className="text-[15px] font-bold text-label">
            {error}
          </p>
          <button
            type="button"
            onClick={() => {
              setError(null)
              setReloadKey((key) => key + 1)
            }}
            className="cursor-pointer rounded-xl bg-primary px-4 py-2 text-[14px] font-bold text-screen"
          >
            다시 시도
          </button>
        </div>
      ) : inquiries === null ? (
        <p className="px-5 py-8 text-center text-[14px] font-medium text-label-alt">문의를 불러오는 중이에요…</p>
      ) : inquiries.length === 0 ? (
        <div className="flex flex-1 flex-col justify-center">
          <EmptyState
            title="아직 남긴 문의가 없어요"
            description="거래·인증·거점에 대해 궁금한 점을 운영자에게 물어보세요. 답변은 이 화면에서 확인할 수 있어요."
          />
        </div>
      ) : (
        <>
          <ul className="border-t border-border">
            {inquiries.map((inquiry) => (
              <li key={inquiry.id}>
                <button
                  type="button"
                  onClick={() => navigate(`/mypage/inquiries/${inquiry.id}`, { state: inquiry })}
                  className="flex w-full cursor-pointer items-center gap-3 border-b border-border px-5 py-4 text-left"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[15px] font-bold text-label">{inquiry.title}</p>
                    <p className="mt-1 text-[12px] font-medium text-label-alt">
                      {INQUIRY_CATEGORY_LABEL[inquiry.category]} · {formatInquiryDate(inquiry.createdAt)}
                    </p>
                  </div>
                  <span
                    className={`flex-none rounded-md px-1.5 py-0.5 text-[11px] font-bold whitespace-nowrap ${INQUIRY_STATUS_BADGE[inquiry.status]}`}
                  >
                    {INQUIRY_STATUS_LABEL[inquiry.status]}
                  </span>
                  <MaterialIcon name="chevron_right" size={18} className="flex-none text-label-alt" />
                </button>
              </li>
            ))}
          </ul>
          {nextPage !== null && (
            <button
              type="button"
              onClick={handleLoadMore}
              disabled={loadingMore}
              className="mx-5 mt-4 h-11 flex-none cursor-pointer rounded-xl border border-border bg-surface text-[14px] font-bold text-body disabled:cursor-default"
            >
              {loadingMore ? '불러오는 중…' : '더 보기'}
            </button>
          )}
          <div className="flex-1" />
        </>
      )}

      {!error && inquiries !== null && (
        <BottomActionBar>
          <PrimaryButton label="문의 작성하기" onClick={() => navigate('/mypage/inquiries/new')} />
        </BottomActionBar>
      )}
    </Screen>
  )
}
