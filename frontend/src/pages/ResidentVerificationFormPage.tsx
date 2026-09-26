import { useState } from 'react'
import AgreementCheckbox from '../components/AgreementCheckbox'
import BottomActionBar from '../components/BottomActionBar'
import DocumentTypeChips from '../components/DocumentTypeChips'
import NoticeBox from '../components/NoticeBox'
import PhotoUploadGrid from '../components/PhotoUploadGrid'
import PrimaryButton from '../components/PrimaryButton'
import ProgressSteps from '../components/ProgressSteps'
import Screen from '../components/Screen'
import TextField from '../components/TextField'
import TopBar from '../components/TopBar'
import type { UploadedFile, VerificationSubmitMeta } from '../types/verification'

const DOC_TYPES = ['임대차 계약서', '공과금 고지서', '주민등록 등본', '기타']
const MAX_FILES = 3

interface ResidentVerificationFormPageProps {
  onBack: () => void
  onSubmitSuccess: (meta: VerificationSubmitMeta) => void
}

/** 서류 첨부 — 월계 주민 (#1c). */
export default function ResidentVerificationFormPage({
  onBack,
  onSubmitSuccess,
}: ResidentVerificationFormPageProps) {
  const [docType, setDocType] = useState<string | null>(null)
  const [files, setFiles] = useState<UploadedFile[]>([])
  const [name, setName] = useState('')
  const [roadAddress, setRoadAddress] = useState('')
  const [agreed, setAgreed] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleAddFiles = (fileList: FileList) => {
    const remaining = MAX_FILES - files.length
    const next: UploadedFile[] = Array.from(fileList)
      .slice(0, remaining)
      .map((file) => ({
        id: crypto.randomUUID(),
        name: file.name,
        size: file.size,
        status: 'done',
      }))
    setFiles((prev) => [...prev, ...next])
  }

  const handleRemoveFile = (id: string) => {
    setFiles((prev) => prev.filter((f) => f.id !== id))
  }

  const handleFindAddress = () => {
    // TODO: 카카오 주소 검색 API 연동 — 지금은 자유 입력 대신 임시 값으로 채웁니다.
    setRoadAddress('서울 노원구 월계로 76')
  }

  const canSubmit =
    Boolean(docType) && files.length > 0 && name.trim().length > 0 && Boolean(roadAddress) && agreed

  const handleSubmit = async () => {
    if (!canSubmit || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      // TODO: POST /verification/documents → POST /verification 연동
      await new Promise((resolve) => setTimeout(resolve, 600))
      onSubmitSuccess({
        type: 'resident',
        label: '월계 주민 인증',
        submittedAt: new Date(),
        docCount: files.length,
      })
    } catch {
      setError('신청에 실패했어요. 잠시 후 다시 시도해 주세요.')
      setSubmitting(false)
    }
  }

  return (
    <Screen>
      <TopBar title="월계 주민 인증" onBack={onBack} />
      <ProgressSteps total={2} current={2} />

      <div className="px-5">
        <h1 className="font-hand text-[26px] leading-[1.25] font-bold text-label">
          거주를 확인할 수 있는 서류를
          <br />
          올려주세요
        </h1>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">
          이름과 주소가 보이면 됩니다. 계약 금액이나 계좌번호 같은 정보는 가리고 찍어도 괜찮아요.
        </p>
      </div>

      <DocumentTypeChips
        label="서류 종류"
        required
        options={DOC_TYPES}
        value={docType}
        onChange={setDocType}
      />

      <PhotoUploadGrid
        files={files}
        max={MAX_FILES}
        thumbIcon="description"
        thumbClassName="bg-sunken text-label-alt"
        onAdd={handleAddFiles}
        onRemove={handleRemoveFile}
        showQuickActions
        helperText="JPG · PNG · PDF, 한 장에 10MB까지"
      />

      <div className="flex flex-col gap-4 px-5 pt-5">
        <TextField label="서류상 이름" required value={name} onChange={setName} placeholder="이름을 입력하세요" />
        <TextField
          label="주소"
          required
          value={roadAddress}
          onChange={setRoadAddress}
          readOnly
          placeholder="서울 노원구 월계로 …"
          rightSlot={
            <button
              type="button"
              onClick={handleFindAddress}
              className="flex-none cursor-pointer text-[13px] font-bold whitespace-nowrap text-accent"
            >
              주소 찾기
            </button>
          }
        />
      </div>

      <NoticeBox
        variant="amber"
        icon="schedule"
        text="검토는 보통 1~2일(평일 기준) 걸려요. 결과는 알림으로 알려드립니다."
      />

      <AgreementCheckbox
        checked={agreed}
        onChange={setAgreed}
        label="인증 확인을 위한 서류 수집·이용에 동의합니다"
      />

      {error && <p className="mx-5 mt-3.5 text-[13px] font-semibold text-terracotta">{error}</p>}

      <div className="flex-1" />

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
