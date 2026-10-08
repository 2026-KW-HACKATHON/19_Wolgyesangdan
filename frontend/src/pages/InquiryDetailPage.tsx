import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getMyInquiries } from '../api/inquiries'
import MaterialIcon from '../components/icons/MaterialIcon'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import {
  formatInquiryDate,
  INQUIRY_CATEGORY_LABEL,
  INQUIRY_STATUS_BADGE,
  INQUIRY_STATUS_LABEL,
} from '../lib/inquiry'
import type { Inquiry } from '../types/inquiry'

// 문의 하나만 조회하는 API가 없어서, 목록에서 넘겨받지 못했을 때(새로고침·주소 직접 입력)는
// 내 문의를 한 페이지에 최대 개수(서버 상한 100)만큼 불러와 그 안에서 찾는다
const LOOKUP_SIZE = 100

type Result = { id: string; inquiry: Inquiry } | { id: string; inquiry: null; message: string }

/** 문의 상세 (/mypage/inquiries/:id, 로그인 필요) — 내용 + 운영자 답변 */
export default function InquiryDetailPage() {
  const navigate = useNavigate()
  const { id = '' } = useParams()
  // 목록에서 눌러 들어오면 그 문의를 router state로 넘겨받는다
  const passed = useLocation().state as Inquiry | null
  const fromList = passed && String(passed.id) === id ? passed : null
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (fromList) return
    let ignore = false
    getMyInquiries(0, LOOKUP_SIZE)
      .then((page) => {
        if (ignore) return
        const found = page.content.find((inquiry) => String(inquiry.id) === id)
        setResult(found ? { id, inquiry: found } : { id, inquiry: null, message: '문의를 찾을 수 없어요' })
      })
      .catch((e: unknown) => {
        if (ignore) return
        // 로그인이 풀린 경우 — apiFetch가 "로그인이 필요해요" 팝업(SessionExpiredDialog)을 띄운다
        if (e instanceof ApiError && e.status === 401) return
        setResult({
          id,
          inquiry: null,
          message: e instanceof ApiError ? e.message : '문의를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
        })
      })
    return () => {
      ignore = true
    }
  }, [id, fromList])

  const current = result?.id === id ? result : null
  const inquiry = fromList ?? current?.inquiry ?? null
  const goList = () => navigate('/mypage/inquiries')

  return (
    <Screen>
      <TopBar title="문의 상세" onBack={() => (fromList ? navigate(-1) : goList())} />

      {inquiry ? (
        <div className="px-5 pb-8">
          <div className="flex items-center gap-2">
            <span className="rounded-md bg-sunken px-1.5 py-0.5 text-[11px] font-bold text-ink-2">
              {INQUIRY_CATEGORY_LABEL[inquiry.category]}
            </span>
            <span
              className={`rounded-md px-1.5 py-0.5 text-[11px] font-bold whitespace-nowrap ${INQUIRY_STATUS_BADGE[inquiry.status]}`}
            >
              {INQUIRY_STATUS_LABEL[inquiry.status]}
            </span>
          </div>
          <h1 className="mt-2.5 text-[19px] leading-[1.35] font-extrabold break-words text-label">{inquiry.title}</h1>
          <p className="mt-1.5 text-[12px] font-medium text-label-alt">{formatInquiryDate(inquiry.createdAt)} 작성</p>

          <div className="mt-4 rounded-2xl border border-border bg-surface px-4 py-3.5 text-[14px] leading-[1.65] font-medium break-words whitespace-pre-wrap text-body">
            {inquiry.content}
          </div>

          {inquiry.answer ? (
            <div className="mt-4 rounded-2xl bg-primary-tint px-4 py-3.5">
              <div className="flex items-center gap-1.5">
                <MaterialIcon name="support_agent" size={18} className="text-accent" />
                <span className="text-[14px] font-bold text-primary-tint-ink">운영자 답변</span>
                {inquiry.answeredAt && (
                  <span className="ml-auto text-[12px] font-medium text-primary-tint-ink opacity-80">
                    {formatInquiryDate(inquiry.answeredAt)}
                  </span>
                )}
              </div>
              <p className="mt-2 text-[14px] leading-[1.65] font-medium break-words whitespace-pre-wrap text-primary-tint-ink">
                {inquiry.answer}
              </p>
            </div>
          ) : (
            <div className="mt-4 flex gap-2.5 rounded-2xl bg-amber-tint px-4 py-3.5">
              <MaterialIcon name="hourglass_top" size={19} className="mt-px flex-none text-amber-ink" />
              <p className="text-[13px] leading-[1.55] font-medium text-terracotta-ink">
                운영자가 확인하고 있어요. 답변이 달리면 이 화면에서 볼 수 있어요.
              </p>
            </div>
          )}
        </div>
      ) : current ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
          <MaterialIcon name="search_off" size={36} className="text-label-alt" />
          <p role="alert" className="text-[15px] font-bold text-label">
            {current.inquiry === null ? current.message : ''}
          </p>
          <button
            type="button"
            onClick={goList}
            className="cursor-pointer rounded-xl bg-primary px-4 py-2 text-[14px] font-bold text-screen"
          >
            문의 목록으로
          </button>
        </div>
      ) : (
        <p className="px-5 py-8 text-center text-[14px] font-medium text-label-alt">문의를 불러오는 중이에요…</p>
      )}
    </Screen>
  )
}
