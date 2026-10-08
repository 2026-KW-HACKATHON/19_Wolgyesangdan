import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/client'
import { createVerification, resolveDocumentContentType, uploadVerificationDocument } from '../api/verification'
import BottomActionBar from '../components/BottomActionBar'
import ChoiceChips from '../components/ChoiceChips'
import MaterialIcon from '../components/icons/MaterialIcon'
import NoticeBox from '../components/NoticeBox'
import PhotoUploadGrid from '../components/PhotoUploadGrid'
import PrimaryButton from '../components/PrimaryButton'
import ProgressSteps from '../components/ProgressSteps'
import Screen from '../components/Screen'
import TextField from '../components/TextField'
import TopBar from '../components/TopBar'
import { PRIORITY_OPTIONS, toDocumentType } from '../data/priorityVerification'
import type { PrioritySubmitMeta, PriorityType, UploadedFile } from '../types/verification'

// 서류는 한 파일만 받는다 — 관리자가 한 장을 보고 승인·반려한다
const MAX_FILES = 1
const MAX_FILE_BYTES = 10 * 1024 * 1024
const MAX_NAME = 50
const DOCUMENT_ACCEPT = 'image/jpeg,image/png,image/webp,image/heic,image/heif,.heic,.heif,application/pdf'

interface DocumentUpload {
  meta: UploadedFile
  /** 업로드가 끝나면 채워지는 S3 위치 — 인증 신청에 담는다 */
  fileKey?: string
}

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
  const [applicantName, setApplicantName] = useState('')
  const [upload, setUpload] = useState<DocumentUpload | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 사진 미리보기용 로컬 주소 — 지우거나 화면을 떠나면 해제한다
  const previewUrlRef = useRef<string | undefined>(undefined)
  const replacePreviewUrl = (url: string | undefined) => {
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current)
    previewUrlRef.current = url
  }
  useEffect(() => () => replacePreviewUrl(undefined), [])

  // 고르자마자 S3에 올린다. 서류는 공개 URL 없이 저장 위치(fileKey)만 받는다
  const handleAddFiles = (fileList: FileList) => {
    const file = fileList[0]
    const contentType = resolveDocumentContentType(file)
    if (!contentType || file.size > MAX_FILE_BYTES) {
      setFileError('사진(JPG · PNG · HEIC 등) 또는 PDF 파일만, 10MB까지 올릴 수 있어요.')
      return
    }
    setFileError(null)

    const url = contentType === 'application/pdf' ? undefined : URL.createObjectURL(file)
    replacePreviewUrl(url)
    const id = crypto.randomUUID()
    setUpload({ meta: { id, name: file.name, size: file.size, status: 'uploading', url } })

    // 올리는 동안 지우거나 다른 파일로 바꿨으면 결과를 반영하지 않는다
    const updateIfCurrent = (patch: Partial<DocumentUpload> & { status: UploadedFile['status'] }) =>
      setUpload((prev) =>
        prev?.meta.id === id ? { meta: { ...prev.meta, status: patch.status }, fileKey: patch.fileKey } : prev,
      )
    uploadVerificationDocument(file, contentType)
      .then((fileKey) => updateIfCurrent({ status: 'done', fileKey }))
      .catch((e) => {
        updateIfCurrent({ status: 'error' })
        setFileError(
          e instanceof ApiError && e.code === 'VERIFICATION_DOCUMENT_UPLOAD_UNAVAILABLE'
            ? '지금은 서류를 올릴 수 없어요. 잠시 후 다시 시도해 주세요.'
            : '서류를 올리지 못했어요. 지우고 다시 첨부해 주세요.',
        )
      })
  }

  const handleRemoveFile = () => {
    replacePreviewUrl(undefined)
    setUpload(null)
    setFileError(null)
  }

  const fileKey = upload?.meta.status === 'done' ? upload.fileKey : undefined
  const canSubmit = Boolean(docType) && applicantName.trim().length > 0 && Boolean(fileKey)

  const handleSubmit = async () => {
    const documentType = docType ? toDocumentType(docType) : null
    if (!canSubmit || !documentType || !fileKey || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const response = await createVerification({
        verificationType: option.verificationType,
        documentType,
        fileKey,
        applicantName: applicantName.trim(),
      })
      onSubmitSuccess({ type, submittedAt: new Date(response.submittedAt), docCount: 1 })
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

      <div className="px-5 pt-4.5">
        <TextField
          label="서류에 적힌 이름"
          required
          value={applicantName}
          onChange={(v) => setApplicantName(v.slice(0, MAX_NAME))}
          placeholder="예) 김하늘"
        />
      </div>

      <PhotoUploadGrid
        label="서류 사진"
        files={upload ? [upload.meta] : []}
        max={MAX_FILES}
        thumbIcon="description"
        thumbClassName={copy.thumbClassName}
        onAdd={handleAddFiles}
        onRemove={handleRemoveFile}
        accept={DOCUMENT_ACCEPT}
        helperText="사진 또는 PDF 1장, 10MB까지"
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
