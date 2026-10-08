import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import BottomActionBar from '../components/BottomActionBar'
import ChoiceChips from '../components/ChoiceChips'
import ContactRequiredSheet from '../components/ContactRequiredSheet'
import MaterialIcon from '../components/icons/MaterialIcon'
import PhotoUploadGrid from '../components/PhotoUploadGrid'
import PrimaryButton from '../components/PrimaryButton'
import TextField from '../components/TextField'
import { getActiveCampaign } from '../api/campaigns'
import { ApiError } from '../api/client'
import { createItem, getCategories, resolveItemImageContentType, uploadItemImage } from '../api/items'
import { useContact } from '../contexts/ContactContext'
import type {
  CategoryCarbon,
  CategoryGroup,
  ConditionGrade as Condition,
  ItemCreateRequest,
  TradeMethod,
  TransportDifficulty,
} from '../types/item'
import type { ActiveCampaign } from '../types/campaign'
import { registerPhotoStore, useRegisterPhotos } from '../lib/registerPhotoStore'
import type { UploadedFile } from '../types/verification'

const CATEGORY_OPTIONS: CategoryGroup[] = ['가구', '가전', '주방', '생활', '기타']

/** 품목 목록에 없는 물건 — 품목 없이 대분류 값으로 계산한다 (#285) */
const OTHER_ITEM_TYPE = 'OTHER'

/** "기타 품목" 칩 이름. 대분류가 기타면 "그 외 물건" */
function otherItemTypeLabel(categoryGroup: CategoryGroup) {
  return categoryGroup === '기타' ? '그 외 물건' : `기타 ${categoryGroup}`
}
const CONDITION_OPTIONS: Condition[] = ['거의 새것', '상태 좋음', '사용감 있음']
const TRANSPORT_OPTIONS: TransportDifficulty[] = ['쉬움', '보통', '어려움']

type TradeChoice = 'DIRECT' | 'CAMPAIGN' | 'BOTH'

const TRADE_CHOICES: { value: TradeChoice; label: string; desc: string; icon: string }[] = [
  { value: 'DIRECT', label: '직거래', desc: '직접 만나 전달', icon: 'group' },
  { value: 'CAMPAIGN', label: '캠페인 거점', desc: '비대면 입고', icon: 'inventory_2' },
  { value: 'BOTH', label: '둘 다 가능', desc: '신청자가 선택', icon: 'swap_horiz' },
]

const TRADE_METHODS: Record<TradeChoice, TradeMethod[]> = {
  DIRECT: ['DIRECT'],
  CAMPAIGN: ['CAMPAIGN'],
  BOTH: ['DIRECT', 'CAMPAIGN'],
}

/** 서버가 받는 사진 형식 (jpeg·png·webp·heic·heif) */
const PHOTO_ACCEPT = 'image/jpeg,image/png,image/webp,image/heic,image/heif,.heic,.heif'

const MAX_PHOTOS = 5
const MAX_NAME = 30
const MAX_DESCRIPTION = 300
const DRAFT_KEY = 'draft:register'

interface RegisterForm {
  name: string
  categoryGroup: CategoryGroup | null
  /** 품목 코드(예: REFRIGERATOR), 목록에 없는 물건이면 OTHER_ITEM_TYPE, 아직 안 골랐으면 null */
  itemType: string | null
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
  itemType: null,
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

/** 임시 저장 — 텍스트 필드. 사진은 lib/registerPhotoStore가 따로 보관한다. */
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

/** 등록 요청 본문. 비운 선택 항목은 보내지 않는다 */
function toCreateRequest(form: RegisterForm, imageUrls: string[], campaignId: number | undefined): ItemCreateRequest {
  const optional = (value: string) => value.trim() || undefined
  const defect = form.defect.trim()
  const tradeMethods = TRADE_METHODS[form.trade!]
  return {
    name: form.name.trim(),
    categoryGroup: form.categoryGroup!,
    itemType: form.itemType && form.itemType !== OTHER_ITEM_TYPE ? form.itemType : undefined,
    conditionGrade: form.condition!,
    description: optional(form.description),
    usagePeriod: optional(form.usagePeriod),
    // 하자 칸에 무언가 적었으면 하자 있음으로 본다
    defectYn: defect.length > 0,
    defectDescription: defect || undefined,
    size: optional(form.size),
    transportDifficulty: form.transport ?? undefined,
    availableFrom: form.pickupStart || undefined,
    availableUntil: form.pickupEnd || undefined,
    disposalDeadline: form.disposeBy || undefined,
    tradeMethods,
    campaignId: tradeMethods.includes('CAMPAIGN') ? campaignId : undefined,
    imageUrls,
  }
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

/** 거점 거래를 할 수 있는 기간 — 캠페인 전체 기간(등록·신청·수령 기간 중 가장 이른 시작일 ~ 가장 늦은 종료일). 날짜는 YYYY-MM-DD */
interface HubPeriod {
  start: string
  end: string
}

function hubPeriodOf(campaign: ActiveCampaign): HubPeriod {
  const starts = [campaign.registrationStartDate, campaign.applicationStartDate, campaign.pickupStartDate]
  const ends = [campaign.registrationEndDate, campaign.applicationEndDate, campaign.pickupEndDate]
  // YYYY-MM-DD는 글자 순서가 곧 날짜 순서다
  return { start: starts.reduce((a, b) => (a < b ? a : b)), end: ends.reduce((a, b) => (a > b ? a : b)) }
}

/**
 * 지금 거점 거래를 고를 수 없는 이유. 고를 수 있으면 null (#268).
 * 서버와 같은 기준 — 캠페인이 끝나지 않았고 오늘이 물품 등록 기간 안(+ 신청 마감 전)이어야 한다 (ItemService.findCampaignAcceptingItems)
 */
function hubUnavailableReason(campaign: ActiveCampaign | null, today: string) {
  if (!campaign || campaign.status === 'ENDED') return '지금은 진행 중인 캠페인이 없어서 직거래만 고를 수 있어요.'
  const accepting =
    today >= campaign.registrationStartDate && today <= campaign.registrationEndDate && today <= campaign.applicationEndDate
  return accepting
    ? null
    : `거점 거래는 캠페인 물품 등록 기간(${monthDay(campaign.registrationStartDate)} ~ ${monthDay(campaign.registrationEndDate)})에만 고를 수 있어요.`
}

const usesHub = (trade: TradeChoice | null) => trade === 'CAMPAIGN' || trade === 'BOTH'

/** "2026-10-08" → "10.8" */
function monthDay(isoDate: string) {
  const [, month, day] = isoDate.split('-').map(Number)
  return `${month}.${day}`
}

/**
 * 거점 거래를 골랐는데 전달 가능 기간이 캠페인 기간을 벗어나면 그 안내 문구 (#256). 문제없으면 null.
 * 서버도 같은 규칙으로 거절한다 (ItemService.validateAvailablePeriodInCampaign). 비워 둔 날짜는 확인하지 않는다
 */
function hubPeriodError(form: RegisterForm, period: HubPeriod | null) {
  if (!period || !usesHub(form.trade)) return null
  const outside = [form.pickupStart, form.pickupEnd].some((date) => date && (date < period.start || date > period.end))
  return outside
    ? `거점 거래는 전달 가능 기간이 캠페인 기간(${monthDay(period.start)} ~ ${monthDay(period.end)}) 안이어야 해요. 기간을 고치거나 직거래를 골라 주세요.`
    : null
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
  const { contact, loading: contactLoading } = useContact()

  const [form, setForm] = useState<RegisterForm>(loadDraft)
  // 사진은 화면 밖 보관소에 둔다 — 연락 수단 설정 화면에 다녀와도 그대로 남는다 (#161)
  const files = useRegisterPhotos()
  const [submitting, setSubmitting] = useState(false)
  const [photoError, setPhotoError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  // 거점 거래로 등록할 때 보낼 캠페인. undefined: 불러오는 중, null: 진행 중·예정 캠페인이 없거나 불러오지 못함
  const [campaign, setCampaign] = useState<ActiveCampaign | null | undefined>(undefined)
  // 카테고리별 탄소 절감 예상치(kg CO₂e)와 품목 목록. 등록 시 서버가 저장하는 값과 같은 참조표라 화면 숫자와 등록 결과가 맞는다.
  // 불러오는 중이거나 실패하면 null — 예상치 박스와 품목 선택을 숨긴다
  const [categories, setCategories] = useState<CategoryCarbon[] | null>(null)

  useEffect(() => {
    let ignore = false
    getCategories()
      .then((loaded) => {
        if (ignore) return
        setCategories(loaded)
        // 임시저장에 남은 품목이 지금 목록에 없으면 선택을 푼다
        setForm((prev) => {
          if (!prev.itemType || prev.itemType === OTHER_ITEM_TYPE) return prev
          const itemTypes = loaded.find((c) => c.categoryGroup === prev.categoryGroup)?.itemTypes ?? []
          return itemTypes.some((t) => t.itemType === prev.itemType) ? prev : { ...prev, itemType: null }
        })
      })
      .catch(() => {
        // 예상치는 참고용이라 실패해도 등록은 그대로 진행할 수 있다
      })
    // 캠페인을 못 불러오면 거점 거래는 고를 수 없는 것으로 본다 (직거래 등록에는 필요 없다)
    const applyCampaign = (loaded: ActiveCampaign | null) => {
      if (ignore) return
      setCampaign(loaded)
      // 임시저장에 거점 거래가 남아 있는데 지금은 고를 수 없으면 선택을 푼다
      if (hubUnavailableReason(loaded, todayIso())) {
        setForm((prev) => (usesHub(prev.trade) ? { ...prev, trade: null } : prev))
      }
    }
    getActiveCampaign()
      .then(applyCampaign)
      .catch(() => applyCampaign(null))
    return () => {
      ignore = true
    }
  }, [])

  const today = todayIso()
  const photosUploading = files.some((f) => f.status === 'uploading')
  const photosFailed = files.some((f) => f.status === 'error')
  // 사진은 전부 올라간 뒤에만 등록할 수 있다 (올리지 못한 사진은 지우고 다시 첨부)
  const hasPhoto = files.length > 0 && !photosUploading && !photosFailed
  // 거점 거래를 고를 수 없는 이유 — 캠페인을 불러오는 동안은 null이지만 그동안은 아래 hubReady로 막는다
  const hubBlocked = campaign === undefined ? null : hubUnavailableReason(campaign, today)
  const hubReady = campaign !== undefined && !hubBlocked
  // 거점 거래를 할 수 있는 기간 — 전달 가능 기간이 이 안인지 확인한다 (#256)
  const hubError = hubPeriodError(form, campaign ? hubPeriodOf(campaign) : null)
  // 고른 대분류의 품목. 품목 기능 이전 서버이거나 목록을 못 불러오면 빈 배열 — 품목 없이 등록한다
  const category = categories?.find((c) => c.categoryGroup === form.categoryGroup) ?? null
  const itemTypes = category?.itemTypes ?? []
  const itemTypeMissing = itemTypes.length > 0 && form.itemType === null
  const valid =
    isFormValid(form, hasPhoto) && !itemTypeMissing && !hubError && (!usesHub(form.trade) || hubReady)
  // 연락 수단이 없으면 들어오자마자 안내한다 — 사진을 다 올린 뒤에야 막히지 않도록 (#161)
  const showSheet = !contactLoading && !contact

  useEffect(() => {
    saveDraft(form)
  }, [form])

  const set = <K extends keyof RegisterForm>(key: K, value: RegisterForm[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  // 고르자마자 한 장씩 S3에 올린다. 등록 요청에는 올라간 사진 주소만 담는다
  const handleAddFiles = (fileList: FileList) => {
    const picked = Array.from(fileList).slice(0, MAX_PHOTOS - files.length)
    const unsupported = picked.filter((file) => !resolveItemImageContentType(file))
    setPhotoError(
      unsupported.length > 0 ? 'JPG, PNG, WEBP, HEIC 사진만 올릴 수 있어요' : null,
    )

    const uploads = picked.flatMap((file) => {
      const contentType = resolveItemImageContentType(file)
      if (!contentType) return []
      const entry: UploadedFile = {
        id: crypto.randomUUID(),
        name: file.name,
        size: file.size,
        status: 'uploading',
        // 올리는 동안에도 바로 사진이 보이도록 로컬 미리보기 주소를 만든다 (지울 때·등록 후에 보관소가 정리)
        url: URL.createObjectURL(file),
      }
      return [{ entry, file, contentType }]
    })
    registerPhotoStore.add(uploads.map((u) => u.entry))

    // 결과는 보관소에 반영한다 — 올리는 중에 다른 화면에 가 있어도 돌아오면 끝난 상태로 보인다
    for (const { entry, file, contentType } of uploads) {
      uploadItemImage(file, contentType)
        .then((imageUrl) => registerPhotoStore.update(entry.id, { status: 'done', imageUrl }))
        .catch((e) => {
          registerPhotoStore.update(entry.id, { status: 'error' })
          setPhotoError(
            e instanceof ApiError && e.code === 'ITEM_IMAGE_UPLOAD_UNAVAILABLE'
              ? '지금은 사진을 올릴 수 없어요. 잠시 후 다시 시도해 주세요'
              : '사진을 올리지 못했어요. 지우고 다시 첨부해 주세요',
          )
        })
    }
  }

  const handleRemoveFile = (id: string) => registerPhotoStore.remove(id)

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (submitting) return
    // 연락 수단이 없으면 안내 시트가 떠 있어 제출할 수 없지만, 불러오는 중에 누른 경우를 막는다
    if (!contact || !valid || !form.trade) return

    setSubmitting(true)
    setSubmitError(null)
    let created: { id: number }
    try {
      const imageUrls = files.flatMap((f) => (f.imageUrl ? [f.imageUrl] : []))
      created = await createItem(toCreateRequest(form, imageUrls, campaign?.id))
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : '등록하지 못했어요. 잠시 후 다시 시도해 주세요.')
      return
    } finally {
      setSubmitting(false)
    }
    setForm(INITIAL_FORM)
    registerPhotoStore.clear()
    setPhotoError(null)
    clearDraft()
    // 방금 등록한 물품 상세로 간다. 뒤로가기로 빈 등록 폼에 돌아오지 않게 등록 화면 기록을 바꿔 끼운다 (#193)
    navigate(`/items/${created.id}`, { replace: true, state: { registered: true } })
  }

  const selectedItemType = itemTypes.find((t) => t.itemType === form.itemType) ?? null
  const carbonKg = selectedItemType?.carbonReductionKg ?? category?.carbonReductionKg ?? null
  const carbonCaption = selectedItemType
    ? `${selectedItemType.basis} 기준 예상치예요`
    : itemTypes.length > 0 && form.itemType === null
      ? `${form.categoryGroup} 평균 예상치예요. 품목을 고르면 더 정확해져요`
      : `${form.categoryGroup} 평균 기준 예상치예요. 등록 후 자동으로 계산돼요`
  const itemTypeLabels = form.categoryGroup
    ? [...itemTypes.map((t) => t.label), otherItemTypeLabel(form.categoryGroup)]
    : []
  const selectedItemTypeLabel =
    form.itemType === OTHER_ITEM_TYPE && form.categoryGroup
      ? otherItemTypeLabel(form.categoryGroup)
      : (selectedItemType?.label ?? null)

  const handleCategoryChange = (categoryGroup: CategoryGroup) =>
    // 대분류를 바꾸면 그 아래 품목도 다시 고른다
    setForm((prev) => (prev.categoryGroup === categoryGroup ? prev : { ...prev, categoryGroup, itemType: null }))

  const handleItemTypeChange = (label: string) => {
    const picked = itemTypes.find((t) => t.label === label)
    set('itemType', picked ? picked.itemType : OTHER_ITEM_TYPE)
  }

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
          accept={PHOTO_ACCEPT}
          helperText="밝은 곳에서 찍은 사진 한 장이면 충분해요 (최대 5장)"
        />
        {photoError && (
          <p role="alert" className="px-5 pt-1.5 text-[12px] font-semibold text-terracotta">
            {photoError}
          </p>
        )}

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
          onChange={handleCategoryChange}
        />

        {itemTypes.length > 0 && (
          <ChoiceChips
            label="품목"
            required
            options={itemTypeLabels}
            value={selectedItemTypeLabel}
            onChange={handleItemTypeChange}
          />
        )}

        {carbonKg !== null && (
          <div className="mx-5 mt-3 flex items-center gap-2.5 rounded-[14px] bg-primary-tint px-3.5 py-[13px]">
            <MaterialIcon name="autorenew" size={20} className="text-accent" />
            <div>
              <div className="text-[17px] font-extrabold text-primary-dark">약 {carbonKg}kg CO₂e 절감</div>
              <div className="mt-0.5 text-[12px] font-medium text-primary-tint-ink opacity-80">
                {carbonCaption}
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
              // 거점이 들어간 선택지는 지금 거점 거래를 할 수 있을 때만 고를 수 있다 (#268)
              const disabled = usesHub(choice.value) && !hubReady
              return (
                <button
                  key={choice.value}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  disabled={disabled}
                  onClick={() => set('trade', choice.value)}
                  className={`flex cursor-pointer items-center gap-3 rounded-[14px] bg-surface px-4 py-3.5 text-left disabled:cursor-not-allowed disabled:opacity-50 ${
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
            {hubBlocked ??
              (usesHub(form.trade) ? '거점 수령은 캠페인 거점을 통해 진행돼요.' : '직거래는 신청자와 직접 만나 전달해요.')}
          </p>
          {hubError && (
            <p role="alert" className="mt-1.5 text-[12px] leading-normal font-semibold text-terracotta">
              {hubError}
            </p>
          )}
        </div>

        <div className="flex-1" />

        <BottomActionBar sticky={false}>
          {submitError && (
            <p role="alert" className="mb-2.5 text-center text-[13px] font-semibold text-terracotta">
              {submitError}
            </p>
          )}
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
        <ContactRequiredSheet onSetup={() => navigate('/settings/contact?next=/register')} />
      )}
    </div>
  )
}
