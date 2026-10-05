import { useState } from 'react'
import { ApiError } from '../api/client'
import { createVerification } from '../api/verification'
import BottomActionBar from '../components/BottomActionBar'
import ChoiceChips from '../components/ChoiceChips'
import MaterialIcon from '../components/icons/MaterialIcon'
import NoticeBox from '../components/NoticeBox'
import PhotoUploadGrid from '../components/PhotoUploadGrid'
import PrimaryButton from '../components/PrimaryButton'
import ProgressSteps from '../components/ProgressSteps'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import { PRIORITY_OPTIONS, toDocumentType } from '../data/priorityVerification'
import type { PrioritySubmitMeta, PriorityType, UploadedFile } from '../types/verification'

const MAX_FILES = 3
const MAX_FILE_BYTES = 10 * 1024 * 1024
const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'application/pdf']

const COPY: Record<PriorityType, { heading: [string, string]; description: string; review: string; thumbClassName: string }> = {
  freshman: {
    heading: ['신입생임을 확인할 수 있는', '서류를 올려주세요'],
    description: '학교 이름과 본인 이름이 보이면 됩니다.',
    review: '검토는 보통 1~2일(평일 기준) 걸려요. 첨부한 서류는 검토가 끝나면 30일 안에 지웁니다.',
    thumbClassName: 'bg-clay-tint text-clay-ink',
  },
  basic: {
    heading: ['수급 여부를 확인할 수 있는', '서류를 올려주세요'],
    description: '본인 이름과 발급일이 보이면 됩니다. 주민등록번호 뒷자리는 가리고 찍어도 괜찮아요.',
    review: '검토는 보통 1~2일(평일 기준) 걸려요. 서류는 담당자 1명만 확인하고, 검토가 끝나면 30일 안에 지웁니다.',
    thumbClassName: 'bg-primary-tint text-accent',
  },
}

interface PriorityDocumentPageProps {
  type: PriorityType
  onBack: () => void
  onSubmitSuccess: (meta: PrioritySubmitMeta) => void
}

/** 서류 첨부 — 신입생(1d, /verify/priority/freshman) · 기초수급자(1e, /verify/priority/basic) */
export default function PriorityDocumentPage({ type, onBack, onSubmitSuccess }: PriorityDocumentPageProps) {
  const option = PRIORITY_OPTIONS[type]
  const copy = COPY[type]
  // 서류 종류가 하나뿐이면(기초수급자) 기본 선택
  const [docType, setDocType] = useState<string | null>(option.docTypes.length === 1 ? option.docTypes[0] : null)
  const [uploads, setUploads] = useState<{ meta: UploadedFile; file: File }[]>([])
  const [fileError, setFileError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleAddFiles = (fileList: FileList) => {
    const picked = Array.from(fileList)
    const valid = picked.filter((f) => ACCEPTED_TYPES.includes(f.type) && f.size <= MAX_FILE_BYTES)
    setFileError(
      valid.length < picked.length ? 'JPG · PNG · PDF 파일만, 한 장에 10MB까지 올릴 수 있어요.' : null,
    )
    const next = valid.slice(0, MAX_FILES - uploads.length).map((file) => ({
      file,
      meta: { id: crypto.randomUUID(), name: file.name, size: file.size, status: 'done' as const },
    }))
    setUploads((prev) => [...prev, ...next])
  }

  const handleRemoveFile = (id: string) => {
    setUploads((prev) => prev.filter((u) => u.meta.id !== id))
  }

  const canSubmit = Boolean(docType) && uploads.length > 0

  const handleSubmit = async () => {
    const documentType = docType ? toDocumentType(docType) : null
    if (!canSubmit || !documentType || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      // 서류 사진은 서버로 보내지 않고 종류만 보낸다 (운영진이 앱 밖에서 확인)
      const response = await createVerification({ verificationType: option.verificationType, documentType })
      onSubmitSuccess({ type, submittedAt: new Date(response.submittedAt), docCount: uploads.length })
    } catch (e) {
      // 실패해도 입력은 유지한다
      setError(
        e instanceof ApiError
          ? (e.errors[0]?.message ?? e.message)
          : '신청에 실패했어요. 잠시 후 다시 시도해 주세요.',
      )
      setSubmitting(false)
    }
  }

  return (
    <Screen>
      <TopBar title={option.title} onBack={onBack} />
      <ProgressSteps total={2} current={2} />

      <div className="px-5">
        <h1 className="font-hand text-[26px] leading-[1.25] font-bold text-label">
          {copy.heading[0]}
          <br />
          {copy.heading[1]}
        </h1>
        <p className="mt-[7px] text-[14px] leading-[1.6] font-medium text-ink-3">{copy.description}</p>
      </div>

      <ChoiceChips label="서류 종류" required options={option.docTypes} value={docType} onChange={setDocType} />

      <PhotoUploadGrid
        files={uploads.map((u) => u.meta)}
        max={MAX_FILES}
        thumbIcon="description"
        thumbClassName={copy.thumbClassName}
        onAdd={handleAddFiles}
        onRemove={handleRemoveFile}
        helperText="JPG · PNG · PDF, 한 장에 10MB까지"
      />
      {fileError && <p className="mx-5 mt-1.5 text-[12px] font-semibold text-terracotta">{fileError}</p>}

      {type === 'basic' && (
        <div className="mx-5 mt-[18px] flex gap-[9px] rounded-2xl bg-primary-tint px-4 py-3.5">
          <MaterialIcon name="visibility_off" size={19} className="mt-px flex-none text-accent" />
          <div className="flex-1">
            <div className="text-[14px] font-bold text-[#2B331F]">거래 상대는 기초수급자인 걸 알 수 없어요</div>
            <div className="mt-1 text-[12px] leading-[1.55] font-medium text-[#57603F]">
              인증 여부는 대기 순번에만 반영되고, 프로필이나 신청 내역 어디에도 표시되지 않아요.
            </div>
          </div>
        </div>
      )}

      <NoticeBox variant="amber" icon="schedule" text={copy.review} />

      {error && (
        <p role="alert" className="mx-5 mt-3.5 text-[13px] font-semibold text-terracotta">
          {error}
        </p>
      )}

      <div className="min-h-5 flex-1" />

      <BottomActionBar>
        <PrimaryButton
          label="인증 신청하기"
          disabled={!canSubmit}
          loading={submitting}
          loadingLabel="신청 중…"
          onClick={handleSubmit}
        />
      </BottomActionBar>
    </Screen>
  )
}
