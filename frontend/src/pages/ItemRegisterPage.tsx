import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import BottomActionBar from '../components/BottomActionBar'
import ChoiceChips from '../components/ChoiceChips'
import ContactRequiredSheet from '../components/ContactRequiredSheet'
import MaterialIcon from '../components/icons/MaterialIcon'
import PhotoUploadGrid from '../components/PhotoUploadGrid'
import PrimaryButton from '../components/PrimaryButton'
import TextField from '../components/TextField'
import Toast from '../components/Toast'
import { getCategories } from '../api/items'
import { useContact } from '../contexts/ContactContext'
import type { CategoryGroup, ConditionGrade as Condition, TransportDifficulty } from '../types/item'
import type { UploadedFile } from '../types/verification'

const CATEGORY_OPTIONS: CategoryGroup[] = ['가구', '가전', '주방', '생활', '기타']
const CONDITION_OPTIONS: Condition[] = ['거의 새것', '상태 좋음', '사용감 있음']
const TRANSPORT_OPTIONS: TransportDifficulty[] = ['쉬움', '보통', '어려움']

type TradeChoice = 'DIRECT' | 'CAMPAIGN' | 'BOTH'

const TRADE_CHOICES: { value: TradeChoice; label: string; desc: string; icon: string }[] = [
  { value: 'DIRECT', label: '직거래', desc: '직접 만나 전달', icon: 'group' },
  { value: 'CAMPAIGN', label: '캠페인 거점', desc: '비대면 입고', icon: 'inventory_2' },
  { value: 'BOTH', label: '둘 다 가능', desc: '신청자가 선택', icon: 'swap_horiz' },
]

const MAX_PHOTOS = 5
const MAX_NAME = 30
const MAX_DESCRIPTION = 300
const DRAFT_KEY = 'draft:register'

interface RegisterForm {
  name: string
  categoryGroup: CategoryGroup | null
  condition: Condition | null
  usagePeriod: string
  description: string
  defect: string
  size: string
  transport: TransportDifficulty | null
  /** 전달 가능 기간 (YYYY-MM-DD) */
  pickupStart: string
  pickupEnd: string
  /** 처분 필요일 (YYYY-MM-DD) */
  disposeBy: string
  trade: TradeChoice | null
}

const INITIAL_FORM: RegisterForm = {
  name: '',
  categoryGroup: null,
  condition: null,
  usagePeriod: '',
  description: '',
  defect: '',
  size: '',
  transport: null,
  pickupStart: '',
  pickupEnd: '',
  disposeBy: '',
  trade: null,
}

/** 임시 저장. 사진(File)은 직렬화할 수 없어서 텍스트 필드만 저장한다. */
function loadDraft(): RegisterForm {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return INITIAL_FORM
    return { ...INITIAL_FORM, ...(JSON.parse(raw) as Partial<RegisterForm>) }
  } catch {
    return INITIAL_FORM
  }
}

function saveDraft(form: RegisterForm) {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(form))
  } catch {
    // 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)에서는 조용히 넘어간다
  }
}

function clearDraft() {
  try {
    localStorage.removeItem(DRAFT_KEY)
  } catch {
    // 위와 같음
  }
}

/** 로컬 기준 오늘 날짜 (YYYY-MM-DD). toISOString은 UTC라 자정 무렵 하루 어긋난다. */
function todayIso() {
  const d = new Date()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}

function isFormValid(form: RegisterForm, hasPhoto: boolean) {
  const rangeOk = !form.pickupStart || !form.pickupEnd || form.pickupStart <= form.pickupEnd
  return (
    hasPhoto &&
    form.name.trim().length > 0 &&
    form.categoryGroup !== null &&
    form.condition !== null &&
    form.trade !== null &&
    rangeOk
  )
}

function FieldLabel({ children, required }: { children: ReactNode; required?: boolean }) {
  return (
    <div className="mb-2 text-[14px] font-bold text-body">
      {children}
      {required && <span className="text-terracotta"> *</span>}
    </div>
  )
}

function DateField({
  id,
  label,
  value,
  min,
  onChange,
}: {
  id: string
  label: string
  value: string
  min?: string
  onChange: (v: string) => void
}) {
  return (
    <div className="flex-1">
      <label htmlFor={id} className="mb-2 block text-[14px] font-bold text-body">
        {label}
      </label>
      <input
        id={id}
        type="date"
        value={value}
        min={min}
        onChange={(e) => onChange(e.target.value)}
        className="h-12 w-full rounded-[14px] border border-border bg-surface px-3.5 text-[14px] font-semibold text-label outline-none"
      />
    </div>
  )
}

/** 물품 등록 (4d, /register). 연락 수단이 없으면 4a 시트를 띄운다. */
export default function ItemRegisterPage() {
  const navigate = useNavigate()
  const { contact } = useContact()

  const [form, setForm] = useState<RegisterForm>(loadDraft)
  const [files, setFiles] = useState<UploadedFile[]>([])
  const [sheetOpen, setSheetOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [toastVisible, setToastVisible] = useState(false)
  // 카테고리별 탄소 절감 예상치(kg CO₂e). 등록 시 서버가 저장하는 값과 같은 참조표라 화면 숫자와 등록 결과가 맞는다.
  // 불러오는 중이거나 실패하면 null — 예상치 박스를 숨긴다
  const [carbonByCategory, setCarbonByCategory] = useState<Partial<Record<CategoryGroup, number>> | null>(null)

  useEffect(() => {
    let ignore = false
    getCategories()
      .then((categories) => {
        if (ignore) return
        setCarbonByCategory(
          Object.fromEntries(categories.map((c) => [c.categoryGroup, c.carbonReductionKg])),
        )
      })
      .catch(() => {
        // 예상치는 참고용이라 실패해도 등록은 그대로 진행할 수 있다
      })
    return () => {
      ignore = true
    }
  }, [])

  const today = todayIso()
  const hasPhoto = files.length > 0
  const valid = isFormValid(form, hasPhoto)
  const showSheet = sheetOpen && !contact

  useEffect(() => {
    saveDraft(form)
  }, [form])

  const set = <K extends keyof RegisterForm>(key: K, value: RegisterForm[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const handleAddFiles = (fileList: FileList) => {
    const next: UploadedFile[] = Array.from(fileList).map((file) => ({
      id: crypto.randomUUID(),
      name: file.name,
      size: file.size,
      status: 'done',
    }))
    setFiles((prev) => [...prev, ...next].slice(0, MAX_PHOTOS))
  }

  const handleRemoveFile = (id: string) => setFiles((prev) => prev.filter((f) => f.id !== id))

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (submitting) return
    // 제출 시점에도 연락 수단을 다시 확인한다
    if (!contact) {
      setSheetOpen(true)
      return
    }
    if (!valid || !form.trade) return

    setSubmitting(true)
    // TODO: POST /items (multipart: 사진 + 필드) 연동
    await new Promise((resolve) => setTimeout(resolve, 600))
    setSubmitting(false)
    setForm(INITIAL_FORM)
    setFiles([])
    clearDraft()
    setToastVisible(true)
    setTimeout(() => setToastVisible(false), 2000)
  }

  const carbonKg = (form.categoryGroup && carbonByCategory?.[form.categoryGroup]) ?? null

  return (
    <div className="relative flex min-h-0 flex-1 flex-col">
      <form onSubmit={handleSubmit} className="flex flex-1 flex-col pb-2">
        <header className="flex h-14 flex-none items-center px-5">
          <h1 className="font-hand text-[24px] font-bold text-label">물품 등록</h1>
        </header>

        <PhotoUploadGrid
          label="물품 사진"
          files={files}
          max={MAX_PHOTOS}
          thumbIcon="image"
          thumbClassName="bg-primary-tint text-accent"
          onAdd={handleAddFiles}
          onRemove={handleRemoveFile}
          showQuickActions
          helperText="밝은 곳에서 찍은 사진 한 장이면 충분해요 (최대 5장)"
        />

        <div className="px-5 pt-4.5">
          <TextField
            label="물품명"
            required
            value={form.name}
            onChange={(v) => set('name', v.slice(0, MAX_NAME))}
            placeholder="예) 전자레인지"
          />
        </div>

        <ChoiceChips
          label="카테고리"
          required
          options={CATEGORY_OPTIONS}
          value={form.categoryGroup}
          onChange={(v) => set('categoryGroup', v)}
        />

        {carbonKg !== null && (
          <div className="mx-5 mt-3 flex items-center gap-2.5 rounded-[14px] bg-primary-tint px-3.5 py-[13px]">
            <MaterialIcon name="autorenew" size={20} className="text-accent" />
            <div>
              <div className="text-[17px] font-extrabold text-primary-dark">약 {carbonKg}kg CO₂e 절감</div>
              <div className="mt-0.5 text-[12px] font-medium text-primary-tint-ink opacity-80">
                카테고리 기준 예상치예요. 등록 후 자동으로 계산돼요
              </div>
            </div>
          </div>
        )}

        <ChoiceChips
          label="상태"
          required
          options={CONDITION_OPTIONS}
          value={form.condition}
          onChange={(v) => set('condition', v)}
        />

        <div className="flex flex-col gap-4 px-5 pt-4.5">
          <TextField
            label="사용 기간"
            value={form.usagePeriod}
            onChange={(v) => set('usagePeriod', v)}
            placeholder="예) 2년 사용"
          />

          <div>
            <label htmlFor="item-description" className="mb-2 block text-[14px] font-bold text-body">
              상세 설명
            </label>
            <textarea
              id="item-description"
              value={form.description}
              onChange={(e) => set('description', e.target.value.slice(0, MAX_DESCRIPTION))}
              placeholder="어떤 물건인지, 왜 내놓는지 편하게 적어 주세요"
              className="min-h-26 w-full resize-none rounded-[14px] border border-border bg-surface p-3.5 text-[15px] leading-[1.5] text-label outline-none placeholder:text-label-alt"
            />
            <div className="mt-1 text-right text-[12px] font-medium text-label-alt">
              {form.description.length} / {MAX_DESCRIPTION}
            </div>
          </div>

          <div>
            <TextField
              label="하자 여부"
              value={form.defect}
              onChange={(v) => set('defect', v)}
              placeholder="예) 모서리 흠집"
            />
          </div>

          <div>
            <TextField
              label="크기"
              value={form.size}
              onChange={(v) => set('size', v)}
              placeholder="예) 48 × 36 × 28cm"
            />
            <div className="mt-1.5 text-[12px] font-medium text-label-alt">가로 × 세로 × 높이</div>
          </div>
        </div>

        <ChoiceChips
          label="운반 난이도"
          options={TRANSPORT_OPTIONS}
          value={form.transport}
          onChange={(v) => set('transport', v)}
        />

        <div className="mt-6 h-2 bg-sunken" />

        <div className="flex flex-col gap-4 px-5 pt-5">
          <div className="flex gap-3">
            <DateField
              id="pickup-start"
              label="전달 가능 기간 (시작)"
              value={form.pickupStart}
              min={today}
              onChange={(v) => set('pickupStart', v)}
            />
            <DateField
              id="pickup-end"
              label="전달 가능 기간 (종료)"
              value={form.pickupEnd}
              min={form.pickupStart || today}
              onChange={(v) => set('pickupEnd', v)}
            />
          </div>
          <DateField
            id="dispose-by"
            label="처분 필요일"
            value={form.disposeBy}
            min={today}
            onChange={(v) => set('disposeBy', v)}
          />
        </div>

        <div className="px-5 pt-4.5">
          <FieldLabel required>거래 방식</FieldLabel>
          <div className="flex flex-col gap-2" role="radiogroup" aria-label="거래 방식">
            {TRADE_CHOICES.map((choice) => {
              const selected = form.trade === choice.value
              return (
                <button
                  key={choice.value}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  onClick={() => set('trade', choice.value)}
                  className={`flex cursor-pointer items-center gap-3 rounded-[14px] bg-surface px-4 py-3.5 text-left ${
                    selected ? 'border-2 border-primary py-[13px]' : 'border border-border'
                  }`}
                >
                  <MaterialIcon name={choice.icon} size={20} className={selected ? 'text-accent' : 'text-label-alt'} />
                  <span className="flex-1">
                    <span className="block text-[15px] font-bold text-label">{choice.label}</span>
                    <span className="mt-px block text-[12px] font-medium text-label-alt">{choice.desc}</span>
                  </span>
                </button>
              )
            })}
          </div>
          <p className="mt-2 text-[12px] font-medium text-label-alt">
            {form.trade === 'CAMPAIGN' || form.trade === 'BOTH'
              ? '거점 수령은 캠페인 거점을 통해 진행돼요.'
              : '직거래는 신청자와 직접 만나 전달해요.'}
          </p>
        </div>

        <div className="flex-1" />

        <BottomActionBar sticky={false}>
          <PrimaryButton
            type="submit"
            label="등록하기"
            disabled={!valid}
            loading={submitting}
            loadingLabel="등록 중…"
          />
        </BottomActionBar>
      </form>

      {showSheet && (
        <ContactRequiredSheet
          onSetup={() => navigate('/settings/contact?next=/register')}
          onLater={() => setSheetOpen(false)}
        />
      )}

      <Toast visible={toastVisible}>물품을 등록했어요</Toast>
    </div>
  )
}
