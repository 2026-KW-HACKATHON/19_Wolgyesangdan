import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { createInquiry } from '../api/inquiries'
import BottomActionBar from '../components/BottomActionBar'
import { chipClassName } from '../components/chipStyles'
import PrimaryButton from '../components/PrimaryButton'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import { INQUIRY_CATEGORIES, INQUIRY_CATEGORY_LABEL } from '../lib/inquiry'
import type { InquiryCategory } from '../types/inquiry'

// 서버 검증과 같은 길이 (InquiryCreateRequest)
const TITLE_MAX = 100
const CONTENT_MAX = 2000

/** 문의 작성 (/mypage/inquiries/new, 로그인 필요) — 카테고리 + 제목 + 내용 → POST /inquiries */
export default function InquiryFormPage() {
  const navigate = useNavigate()
  const [category, setCategory] = useState<InquiryCategory | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const canSubmit = category !== null && title.trim() !== '' && content.trim() !== ''

  const handleSubmit = async () => {
    if (!canSubmit || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      await createInquiry({ category, title: title.trim(), content: content.trim() })
      // 작성 화면으로 뒤로가기해서 같은 문의를 또 보내지 않도록 기록을 교체한다
      navigate('/mypage/inquiries', { replace: true })
    } catch (e) {
      // 로그인이 풀린 경우 — apiFetch가 "로그인이 필요해요" 팝업(SessionExpiredDialog)을 띄운다
      if (!(e instanceof ApiError && e.status === 401)) {
        setError(
          e instanceof ApiError
            ? (e.errors[0]?.message ?? e.message)
            : '문의를 보내지 못했어요. 잠시 후 다시 시도해 주세요.',
        )
      }
      setSubmitting(false)
    }
  }

  return (
    <Screen>
      <TopBar title="문의 작성" onBack={() => navigate(-1)} />

      <div className="px-5">
        <h1 className="font-hand text-[26px] leading-[1.25] font-bold text-label">무엇이 궁금하세요?</h1>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">
          운영자가 확인하고 답변을 남겨요. 알림은 따로 가지 않으니 답변은 문의하기 화면에서 확인해 주세요.
        </p>
      </div>

      <div className="px-5 pt-5">
        <div className="mb-[9px] text-[14px] font-bold text-body">
          카테고리<span className="text-terracotta"> *</span>
        </div>
        <div className="flex flex-wrap gap-1.5" role="radiogroup" aria-label="카테고리">
          {INQUIRY_CATEGORIES.map((option) => (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={option === category}
              onClick={() => setCategory(option)}
              className={chipClassName(option === category)}
            >
              {INQUIRY_CATEGORY_LABEL[option]}
            </button>
          ))}
        </div>
      </div>

      <div className="px-5 pt-5">
        <label htmlFor="inquiry-title" className="mb-2 block text-[14px] font-bold text-body">
          제목<span className="text-terracotta"> *</span>
        </label>
        <input
          id="inquiry-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={TITLE_MAX}
          placeholder="예: 거점 운영 시간이 궁금해요"
          className="h-12 w-full rounded-[14px] border border-border bg-surface px-3.5 text-[15px] font-semibold text-label outline-none placeholder:font-medium placeholder:text-label-alt"
        />
      </div>

      <div className="px-5 pt-5 pb-5">
        <label htmlFor="inquiry-content" className="mb-2 block text-[14px] font-bold text-body">
          내용<span className="text-terracotta"> *</span>
        </label>
        <textarea
          id="inquiry-content"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          maxLength={CONTENT_MAX}
          rows={7}
          placeholder="궁금한 점이나 불편했던 점을 자세히 적어 주세요"
          className="w-full resize-none rounded-[14px] border border-border bg-surface px-3.5 py-3 text-[15px] leading-[1.55] font-medium text-label outline-none placeholder:text-label-alt"
        />
        <p className="mt-1 text-right text-[12px] font-medium text-label-alt">
          {content.length.toLocaleString()} / {CONTENT_MAX.toLocaleString()}
        </p>
      </div>

      <div className="flex-1" />

      <BottomActionBar>
        {error && (
          <p role="alert" className="mb-2.5 text-center text-[13px] font-semibold text-terracotta">
            {error}
          </p>
        )}
        <PrimaryButton
          label="문의 보내기"
          disabled={!canSubmit}
          loading={submitting}
          loadingLabel="보내는 중…"
          onClick={handleSubmit}
        />
      </BottomActionBar>
    </Screen>
  )
}
