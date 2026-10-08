import { useEffect, useId, useState, type FormEvent, type ReactNode } from 'react'
import { createAdminCampaign, getAdminCampaigns, updateAdminCampaign } from '../api/campaigns'
import { ApiError } from '../api/client'
import Card from '../components/Card'
import MaterialIcon from '../components/MaterialIcon'
import type {
  AdminCampaign,
  AdminCampaignCreateRequest,
  AdminCampaignList,
  AdminCampaignUpdateRequest,
  PastCampaign,
} from '../types/campaign'

/** 폼에 들고 있는 값. 날짜는 YYYY-MM-DD, 비어 있으면 '' */
interface CampaignForm {
  /** 운영 중 토글 */
  active: boolean
  name: string
  description: string
  registrationStartDate: string
  registrationEndDate: string
  applicationStartDate: string
  applicationEndDate: string
  pickupStartDate: string
  pickupEndDate: string
  locationName: string
  locationAddress: string
  hubHours: string
}

type TextField = Exclude<keyof CampaignForm, 'active'>

const EMPTY_FORM: CampaignForm = {
  active: true,
  name: '',
  description: '',
  registrationStartDate: '',
  registrationEndDate: '',
  applicationStartDate: '',
  applicationEndDate: '',
  pickupStartDate: '',
  pickupEndDate: '',
  locationName: '',
  locationAddress: '',
  hubHours: '',
}

/** 기간 3쌍 — 날짜 6개 구조 그대로 */
const PERIODS: { label: string; start: TextField; end: TextField }[] = [
  { label: '물품 등록 기간', start: 'registrationStartDate', end: 'registrationEndDate' },
  { label: '신청 기간', start: 'applicationStartDate', end: 'applicationEndDate' },
  { label: '수령 기간', start: 'pickupStartDate', end: 'pickupEndDate' },
]

/** 만들 때 꼭 있어야 하는 값 (소개 문구·거점 운영 시간만 선택) */
const REQUIRED: TextField[] = [
  'name',
  'registrationStartDate',
  'registrationEndDate',
  'applicationStartDate',
  'applicationEndDate',
  'pickupStartDate',
  'pickupEndDate',
  'locationName',
  'locationAddress',
]

const TEXT_FIELDS = Object.keys(EMPTY_FORM).filter((field) => field !== 'active') as TextField[]

function toForm(campaign: AdminCampaign): CampaignForm {
  return {
    active: campaign.status !== 'ENDED',
    name: campaign.name,
    description: campaign.description ?? '',
    registrationStartDate: campaign.registrationStartDate,
    registrationEndDate: campaign.registrationEndDate,
    applicationStartDate: campaign.applicationStartDate,
    applicationEndDate: campaign.applicationEndDate,
    pickupStartDate: campaign.pickupStartDate,
    pickupEndDate: campaign.pickupEndDate,
    locationName: campaign.locationName,
    locationAddress: campaign.locationAddress,
    hubHours: campaign.hubHours ?? '',
  }
}

/** YYYY-MM-DD → "2026.3.1" */
function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return `${year}.${month}.${day}`
}

const INPUT_CLASS =
  'h-10 w-full rounded-[10px] border border-border bg-surface px-3 text-[14px] text-label outline-none focus:border-primary'

function Field({ label, htmlFor, children }: { label: string; htmlFor: string; children: ReactNode }) {
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-1.5 block text-[13px] font-bold text-body">
        {label}
      </label>
      {children}
    </div>
  )
}

interface CampaignFormCardProps {
  /** 수정할 캠페인. null이면 새 캠페인 만들기 */
  campaign: AdminCampaign | null
  /** 저장(또는 생성)한 뒤 — 목록을 다시 불러와야 하면 부모가 처리한다 */
  onSaved: (saved: AdminCampaign) => void
  /** 새 캠페인 만들기를 그만둘 때 */
  onCancelCreate: () => void
}

/** 현재 캠페인 수정 폼이자 새 캠페인 만들기 폼 */
function CampaignFormCard({ campaign, onSaved, onCancelCreate }: CampaignFormCardProps) {
  const idPrefix = useId()
  // 마지막으로 저장된 값 — 이것과 달라진 필드만 보낸다
  const [baseline, setBaseline] = useState<CampaignForm>(() => (campaign ? toForm(campaign) : EMPTY_FORM))
  const [form, setForm] = useState<CampaignForm>(baseline)
  const [submitting, setSubmitting] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<{ message: string; fields: string[] } | null>(null)

  const creating = campaign === null
  const changedFields = TEXT_FIELDS.filter((field) => form[field].trim() !== baseline[field].trim())
  const dirty = changedFields.length > 0 || form.active !== baseline.active
  const missingRequired = REQUIRED.some((field) => !form[field].trim())
  const canSubmit = !submitting && !missingRequired && (creating || dirty)

  const change = (patch: Partial<CampaignForm>) => {
    setForm((current) => ({ ...current, ...patch }))
    setSaved(false)
    setError(null)
  }

  const handleCancel = () => {
    if (creating) {
      onCancelCreate()
      return
    }
    setForm(baseline)
    setError(null)
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    setError(null)
    try {
      let result: AdminCampaign
      if (campaign) {
        const request: AdminCampaignUpdateRequest = {}
        for (const field of changedFields) request[field] = form[field].trim()
        if (form.active !== baseline.active) request.status = form.active ? 'ACTIVE' : 'ENDED'
        result = await updateAdminCampaign(campaign.id, request)
      } else {
        const request: AdminCampaignCreateRequest = {
          name: form.name.trim(),
          registrationStartDate: form.registrationStartDate,
          registrationEndDate: form.registrationEndDate,
          applicationStartDate: form.applicationStartDate,
          applicationEndDate: form.applicationEndDate,
          pickupStartDate: form.pickupStartDate,
          pickupEndDate: form.pickupEndDate,
          locationName: form.locationName.trim(),
          locationAddress: form.locationAddress.trim(),
        }
        if (form.description.trim()) request.description = form.description.trim()
        if (form.hubHours.trim()) request.hubHours = form.hubHours.trim()
        result = await createAdminCampaign(request)
      }
      const next = toForm(result)
      setBaseline(next)
      setForm(next)
      setSaved(true)
      onSaved(result)
    } catch (err) {
      setError(
        err instanceof ApiError
          ? { message: err.message, fields: err.errors.map((fieldError) => `${fieldError.field}: ${fieldError.message}`) }
          : { message: '저장하지 못했어요. 잠시 후 다시 시도해 주세요.', fields: [] },
      )
    } finally {
      setSubmitting(false)
    }
  }

  const textInput = (field: TextField, props: { type?: string; placeholder?: string; maxLength?: number } = {}) => (
    <input
      id={`${idPrefix}-${field}`}
      value={form[field]}
      onChange={(e) => change({ [field]: e.target.value })}
      className={INPUT_CLASS}
      {...props}
    />
  )

  return (
    <Card className="p-6">
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <div className="flex items-center justify-between gap-4">
          <h2 className="text-[16px] font-extrabold text-label">{creating ? '새 캠페인' : '현재 캠페인'}</h2>
          {!creating && (
            <div className="flex items-center gap-2.5">
              <span id={`${idPrefix}-active-label`} className="text-[13px] font-bold text-body">
                운영 중
              </span>
              <button
                type="button"
                role="switch"
                aria-checked={form.active}
                aria-labelledby={`${idPrefix}-active-label`}
                onClick={() => change({ active: !form.active })}
                className={`relative h-6 w-11 rounded-full transition-colors ${form.active ? 'bg-primary' : 'bg-border-deep'}`}
              >
                <span
                  className={`absolute top-0.5 left-0.5 h-5 w-5 rounded-full bg-surface transition-transform ${
                    form.active ? 'translate-x-5' : ''
                  }`}
                />
              </button>
            </div>
          )}
        </div>

        {!creating && !form.active && (
          <p className="rounded-[10px] bg-amber-tint px-3.5 py-2.5 text-[13px] font-semibold text-terracotta-ink">
            저장하면 이 캠페인이 끝나요. 회원 앱에서 캠페인이 사라지고, 거점 거래 물품을 새로 등록할 수 없어요.
          </p>
        )}

        <Field label="캠페인 이름" htmlFor={`${idPrefix}-name`}>
          {textInput('name', { maxLength: 100 })}
        </Field>

        <Field label="소개 문구" htmlFor={`${idPrefix}-description`}>
          <textarea
            id={`${idPrefix}-description`}
            value={form.description}
            onChange={(e) => change({ description: e.target.value })}
            rows={3}
            className={`${INPUT_CLASS} h-auto resize-none py-2.5`}
          />
        </Field>

        <div className="flex flex-col gap-3">
          {PERIODS.map((period) => (
            <div key={period.start}>
              <p className="mb-1.5 text-[13px] font-bold text-body">{period.label}</p>
              <div className="flex items-center gap-2">
                <input
                  type="date"
                  aria-label={`${period.label} 시작일`}
                  value={form[period.start]}
                  onChange={(e) => change({ [period.start]: e.target.value })}
                  className={INPUT_CLASS}
                />
                <span className="text-[13px] text-label-alt">~</span>
                <input
                  type="date"
                  aria-label={`${period.label} 종료일`}
                  value={form[period.end]}
                  onChange={(e) => change({ [period.end]: e.target.value })}
                  className={INPUT_CLASS}
                />
              </div>
            </div>
          ))}
        </div>

        <div className="flex flex-col gap-3 border-t border-row-line pt-5">
          <p className="text-[14px] font-extrabold text-label">거점</p>
          <Field label="거점 이름" htmlFor={`${idPrefix}-locationName`}>
            {textInput('locationName', { maxLength: 100 })}
          </Field>
          <Field label="주소" htmlFor={`${idPrefix}-locationAddress`}>
            {textInput('locationAddress', { maxLength: 255 })}
          </Field>
          <Field label="운영 시간" htmlFor={`${idPrefix}-hubHours`}>
            {textInput('hubHours', { maxLength: 100, placeholder: '예: 평일 10:00 ~ 18:00' })}
          </Field>
        </div>

        {error && (
          <div role="alert" className="text-[13px] font-semibold text-terracotta">
            <p>{error.message}</p>
            {error.fields.map((field) => (
              <p key={field} className="mt-0.5 font-medium">
                {field}
              </p>
            ))}
          </div>
        )}

        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={handleCancel}
            disabled={submitting || (!creating && !dirty)}
            className="h-10 rounded-[10px] border border-border bg-surface px-4 text-[14px] font-bold text-body disabled:opacity-50"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={!canSubmit}
            className="h-10 min-w-[104px] rounded-[10px] bg-primary px-4 text-[14px] font-bold text-surface disabled:opacity-50"
          >
            {submitting ? '저장 중…' : saved && !dirty ? '저장됨' : creating ? '만들기' : '저장하기'}
          </button>
        </div>
      </form>
    </Card>
  )
}

function PastCampaignList({ campaigns }: { campaigns: PastCampaign[] }) {
  return (
    <Card>
      <h2 className="px-5 pt-5 pb-3 text-[14px] font-extrabold text-label">지난 캠페인</h2>
      {campaigns.length === 0 ? (
        <p className="px-5 pb-6 text-[13px] text-label-alt">지난 캠페인이 없어요</p>
      ) : (
        <ul>
          {campaigns.map((campaign) => (
            <li key={campaign.id} className="border-t border-row-line px-5 py-3.5">
              <p className="truncate text-[14px] font-bold text-label">{campaign.name}</p>
              <p className="mt-0.5 text-[12px] text-label-alt">
                {formatDate(campaign.startDate)} ~ {formatDate(campaign.endDate)}
              </p>
              <p className="mt-1 text-[12px] font-semibold text-body">
                거래 {campaign.reusedCount.toLocaleString()}건 · 절감 {campaign.carbonReductionKg.toLocaleString()}kg
              </p>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

type Result = { key: number; data: AdminCampaignList } | { key: number; data: null; message: string }

/** 캠페인 (#210) — 현재 캠페인 수정 · 새 캠페인 만들기 · 지난 캠페인 목록 */
export default function CampaignsPage() {
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<Result | null>(null)
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    let ignore = false
    getAdminCampaigns()
      .then((data) => {
        if (!ignore) setResult({ key: reloadKey, data })
      })
      .catch((e: unknown) => {
        if (ignore) return
        setResult({
          key: reloadKey,
          data: null,
          message: e instanceof ApiError ? e.message : '캠페인을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
        })
      })
    return () => {
      ignore = true
    }
  }, [reloadKey])

  // 다시 불러오는 동안에는 이전 결과를 그대로 보여 준다 (저장 직후 폼이 깜빡이지 않게)
  if (!result) {
    return (
      <Card className="px-6 py-16 text-center">
        <p className="text-[14px] font-medium text-label-alt">캠페인을 불러오는 중이에요…</p>
      </Card>
    )
  }

  if (!result.data) {
    return (
      <Card className="flex flex-col items-center gap-3 px-6 py-16 text-center">
        <MaterialIcon name="error" size={32} className="text-label-alt" />
        <p role="alert" className="text-[14px] font-bold text-label">
          {result.message}
        </p>
        <button
          type="button"
          onClick={() => {
            setResult(null)
            setReloadKey((value) => value + 1)
          }}
          className="h-9 rounded-lg bg-primary px-4 text-[13px] font-bold text-surface"
        >
          다시 시도
        </button>
      </Card>
    )
  }

  const { current, past } = result.data
  const reload = () => setReloadKey((value) => value + 1)

  const handleSaved = (saved: AdminCampaign) => {
    // 새로 만들었거나 운영 중을 꺼서 끝난 경우에는 현재·지난 캠페인이 바뀌므로 다시 불러온다
    if (creating || saved.status === 'ENDED') {
      setCreating(false)
      reload()
    }
  }

  return (
    <div className="grid grid-cols-[minmax(0,1fr)_340px] items-start gap-5">
      {current ? (
        <CampaignFormCard key={current.id} campaign={current} onSaved={handleSaved} onCancelCreate={() => {}} />
      ) : creating ? (
        <CampaignFormCard key="new" campaign={null} onSaved={handleSaved} onCancelCreate={() => setCreating(false)} />
      ) : (
        <Card className="flex flex-col items-center gap-2 px-6 py-16 text-center">
          <MaterialIcon name="campaign" size={32} className="text-label-alt" />
          <p className="text-[15px] font-bold text-label">진행 중인 캠페인이 없어요</p>
          <p className="text-[13px] text-label-alt">새 캠페인을 만들면 회원 앱에 캠페인과 거점이 보여요</p>
        </Card>
      )}

      <div className="flex flex-col gap-4">
        <button
          type="button"
          onClick={() => setCreating(true)}
          disabled={current !== null || creating}
          title={current ? '예정이거나 진행 중인 캠페인이 있으면 새로 만들 수 없어요' : undefined}
          className="flex h-11 items-center justify-center gap-1.5 rounded-[10px] bg-admin-sidebar text-[14px] font-bold text-surface disabled:opacity-40"
        >
          <MaterialIcon name="add" size={18} />새 캠페인 만들기
        </button>
        {current && (
          <p className="-mt-2 text-[12px] text-label-alt">
            지금 캠페인의 운영 중을 끄고 저장하면 새 캠페인을 만들 수 있어요
          </p>
        )}
        <PastCampaignList campaigns={past} />
      </div>
    </div>
  )
}
