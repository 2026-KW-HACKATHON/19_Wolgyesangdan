import { useState } from 'react'
import AgreementCheckbox from '../components/AgreementCheckbox'
import BottomActionBar from '../components/BottomActionBar'
import DocumentTypeChips from '../components/DocumentTypeChips'
import MaterialIcon from '../components/icons/MaterialIcon'
import PhotoUploadGrid from '../components/PhotoUploadGrid'
import PrimaryButton from '../components/PrimaryButton'
import ProgressSteps from '../components/ProgressSteps'
import Screen from '../components/Screen'
import TextField from '../components/TextField'
import TopBar from '../components/TopBar'
import type { UploadedFile, VerificationSubmitMeta } from '../types/verification'

const DOC_TYPES = ['학생증', '재학증명서', '합격통지서']
const MAX_FILES = 3
const SCHOOL = '광운대학교'
const STUDENT_ID_PATTERN = /^\d{4,12}$/

interface StudentVerificationFormPageProps {
  onBack: () => void
  onSubmitSuccess: (meta: VerificationSubmitMeta) => void
}

/** 서류 첨부 — 학생 (#1d, 폼 부분). MVP에서는 학교가 광운대학교로 고정됩니다. */
export default function StudentVerificationFormPage({
  onBack,
  onSubmitSuccess,
}: StudentVerificationFormPageProps) {
  const [docType, setDocType] = useState<string | null>(null)
  const [files, setFiles] = useState<UploadedFile[]>([])
  const [department, setDepartment] = useState('')
  const [studentId, setStudentId] = useState('')
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

  const studentIdValid = studentId.length === 0 || STUDENT_ID_PATTERN.test(studentId)

  const canSubmit = Boolean(docType) && files.length > 0 && agreed && studentIdValid

  const handleSubmit = async () => {
    if (!canSubmit || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      // TODO: POST /verification/documents → POST /verification 연동
      await new Promise((resolve) => setTimeout(resolve, 600))
      onSubmitSuccess({
        type: 'student',
        label: `학생 인증 · ${SCHOOL}`,
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
      <TopBar title="학생 인증" onBack={onBack} />
      <ProgressSteps total={2} current={2} />

      <div className="px-5">
        <h1 className="font-hand text-[26px] leading-[1.25] font-bold text-label">
          재학을 확인할 수 있는 서류를
          <br />
          올려주세요
        </h1>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">학교 이름, 본인 이름, 학번이 보이면 됩니다.</p>
      </div>

      <DocumentTypeChips
        label="서류 종류"
        required
        options={DOC_TYPES}
        value={docType}
        onChange={setDocType}
      />

      <div className="flex flex-col gap-4 px-5 pt-5">
        <div>
          <label className="mb-2 block text-[14px] font-bold text-body">
            학교<span className="text-terracotta"> *</span>
          </label>
          <div className="flex h-12 items-center gap-2 rounded-[14px] border border-border bg-surface px-3.5 text-[15px] font-semibold text-label">
            <span className="flex-1">{SCHOOL}</span>
            <MaterialIcon name="expand_more" size={20} className="text-label-alt" />
          </div>
        </div>
        <div className="flex gap-3">
          <TextField
            label="학과"
            value={department}
            onChange={setDepartment}
            placeholder="예) 산업디자인"
          />
          <TextField
            label="학번"
            value={studentId}
            onChange={setStudentId}
            placeholder="예) 2026xxxx"
            inputMode="numeric"
          />
        </div>
        {!studentIdValid && (
          <p className="text-[13px] font-semibold text-terracotta">학번은 숫자 4~12자로 입력해 주세요.</p>
        )}
      </div>

      <PhotoUploadGrid
        files={files}
        max={MAX_FILES}
        thumbIcon="badge"
        thumbClassName="bg-clay-tint text-clay-ink"
        onAdd={handleAddFiles}
        onRemove={handleRemoveFile}
        showQuickActions
        helperText="JPG · PNG · PDF, 한 장에 10MB까지"
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
