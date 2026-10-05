import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import BottomActionBar from '../components/BottomActionBar'
import MaterialIcon from '../components/icons/MaterialIcon'
import PrimaryButton from '../components/PrimaryButton'
import Screen from '../components/Screen'
import TopBar from '../components/TopBar'
import { useContact } from '../contexts/ContactContext'
import type { ContactType } from '../types/contact'

const OPENCHAT_PATTERN = /^https:\/\/open\.kakao\.com\/o\/[A-Za-z0-9]+$/
const PHONE_PATTERN = /^010-\d{4}-\d{4}$/

/** 숫자만 받아서 010-XXXX-XXXX 형태로 맞춘다. */
function formatPhone(raw: string) {
  const digits = raw.replace(/\D/g, '').slice(0, 11)
  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

/** 4b 오픈채팅 / 4c 전화번호 설정. 같은 화면에서 모드만 토글한다. */
export default function ContactSettingsPage() {
  const navigate = useNavigate()
  const { loading } = useContact()

  // 저장된 연락 수단을 받아 온 뒤에 폼을 그려야 입력칸이 그 값으로 채워진다
  if (loading) {
    return (
      <Screen>
        <TopBar title="연락 수단 설정" onBack={() => navigate(-1)} />
        <p className="px-5 py-8 text-center text-[14px] font-medium text-label-alt">연락 수단을 불러오는 중이에요…</p>
      </Screen>
    )
  }

  return <ContactSettingsForm />
}

function ContactSettingsForm() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const next = params.get('next') ?? '/mypage'
  const { contact, saveContact } = useContact()

  const [mode, setMode] = useState<ContactType>(contact?.type ?? 'openchat')
  const [link, setLink] = useState(contact?.type === 'openchat' ? contact.value : '')
  const [phone, setPhone] = useState(contact?.type === 'phone' ? contact.value : '')
  const [verified, setVerified] = useState(contact?.type === 'phone' ? contact.verified : false)
  const [pasteFailed, setPasteFailed] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)

  const linkValid = OPENCHAT_PATTERN.test(link.trim())
  const phoneValid = PHONE_PATTERN.test(phone)
  const canSave = mode === 'openchat' ? linkValid : phoneValid && verified

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText()
      setLink(text.trim())
      setPasteFailed(false)
    } catch {
      setPasteFailed(true)
    }
  }

  const handleVerify = () => {
    // TODO: POST /me/phone/sms → POST /me/phone/verify 연동 (SMS 인증)
    setVerified(true)
  }

  // 서버에 저장한 뒤에 다음 화면으로 넘어간다 (PUT /users/me/contact)
  const handleSave = async () => {
    if (!canSave || saving) return
    setSaving(true)
    setSaveError(null)
    try {
      await saveContact(
        mode === 'openchat'
          ? { type: 'openchat', value: link.trim(), verified: true }
          : { type: 'phone', value: phone, verified: true },
      )
      navigate(next, { replace: true })
    } catch (e) {
      setSaveError(
        e instanceof ApiError
          ? (e.errors[0]?.message ?? e.message)
          : '저장하지 못했어요. 잠시 후 다시 시도해 주세요.',
      )
      setSaving(false)
    }
  }

  return (
    <Screen>
      <TopBar title="연락 수단 설정" onBack={() => navigate(-1)} />

      <div className="px-5">
        <h1 className="font-hand text-[26px] leading-[1.25] font-bold text-label">
          {mode === 'openchat' ? (
            <>
              배정된 이웃과
              <br />
              어떻게 연락할까요?
            </>
          ) : (
            <>
              전화번호로
              <br />
              연락받을게요
            </>
          )}
        </h1>
        <p className="mt-1.5 text-[14px] leading-[1.6] font-medium text-ink-3">
          {mode === 'openchat'
            ? '앱 안에 채팅이 아직 없어요. 오픈채팅방 링크를 등록해두면 번호를 알려주지 않아도 됩니다.'
            : '물품이 배정된 이웃에게만 번호가 전달됩니다.'}
        </p>
      </div>

      {mode === 'openchat' ? (
        <>
          <div className="mx-5 mt-4.5 rounded-[18px] border border-border bg-surface px-[18px] py-4">
            <div className="flex items-center gap-[7px]">
              <MaterialIcon name="lightbulb" size={18} className="text-amber-ink" />
              <span className="text-[15px] font-bold text-label">오픈채팅방 만드는 법</span>
              <span className="ml-auto text-[12px] font-semibold text-label-alt">30초</span>
            </div>
            <ol className="mt-3.5 list-decimal space-y-2 pl-5 text-[13px] leading-normal font-medium text-body">
              <li>카톡 채팅 탭에서 <b>＋ → 오픈채팅</b></li>
              <li>방 이름은 <b>물품 나눔</b>처럼 간단히</li>
              <li>방 설정에서 <b>링크 복사</b> 후 붙여넣기</li>
            </ol>
          </div>

          <div className="px-5 pt-5">
            <label htmlFor="openchat-link" className="mb-2 block text-[14px] font-bold text-body">
              오픈채팅방 링크<span className="text-terracotta"> *</span>
            </label>
            <div className="flex h-12 items-center gap-2 rounded-[14px] border border-border bg-surface px-3.5">
              <MaterialIcon name="link" size={18} className="text-label-alt" />
              <input
                id="openchat-link"
                value={link}
                onChange={(e) => setLink(e.target.value)}
                placeholder="https://open.kakao.com/o/…"
                className="min-w-0 flex-1 bg-transparent text-[15px] font-medium text-label outline-none placeholder:text-label-alt"
              />
              <button
                type="button"
                onClick={handlePaste}
                className="flex-none cursor-pointer text-[13px] font-bold text-accent"
              >
                붙여넣기
              </button>
            </div>
            {link && !linkValid && (
              <p className="mt-1.5 text-[12px] font-semibold text-terracotta">오픈채팅방 링크 형식이 아니에요</p>
            )}
            {pasteFailed && (
              <p className="mt-1.5 text-[12px] font-semibold text-terracotta">클립보드에 접근할 수 없어요. 직접 붙여넣어 주세요</p>
            )}
            <p className="mt-1.5 text-[12px] leading-normal font-medium text-label-alt">
              링크는 배정된 상대에게만 보여요. 물품 목록에는 나타나지 않습니다.
            </p>
          </div>

          <div className="flex justify-center px-5 pt-4">
            <button
              type="button"
              onClick={() => setMode('phone')}
              className="cursor-pointer text-[14px] font-bold text-accent underline underline-offset-[3px]"
            >
              괜찮아요, 그냥 전화번호로 등록할게요
            </button>
          </div>
        </>
      ) : (
        <>
          <div className="px-5 pt-5">
            <label htmlFor="phone-input" className="mb-2 block text-[14px] font-bold text-body">
              전화번호<span className="text-terracotta"> *</span>
            </label>
            <div
              className={`flex h-12 items-center gap-2 rounded-[14px] bg-surface px-3.5 ${
                phoneValid ? 'border-2 border-primary' : 'border border-border'
              }`}
            >
              <MaterialIcon name="call" size={18} className="text-accent" />
              <input
                id="phone-input"
                inputMode="numeric"
                value={phone}
                onChange={(e) => {
                  setPhone(formatPhone(e.target.value))
                  setVerified(false)
                }}
                placeholder="010-0000-0000"
                className="min-w-0 flex-1 bg-transparent text-[15px] font-semibold text-label outline-none placeholder:font-medium placeholder:text-label-alt"
              />
              <button
                type="button"
                onClick={handleVerify}
                disabled={!phoneValid || verified}
                className="flex-none cursor-pointer text-[13px] font-bold text-accent disabled:cursor-default disabled:text-ink-disabled"
              >
                {verified ? '인증 완료' : '인증'}
              </button>
            </div>
          </div>

          <div className="mx-5 mt-3.5 flex gap-2.5 rounded-2xl bg-primary-tint px-4 py-3.5">
            <MaterialIcon name="lock" size={19} className="mt-px flex-none text-accent" />
            <div>
              <div className="text-[14px] font-bold text-primary-tint-ink">매칭된 상대에게만 번호가 공개됩니다</div>
              <div className="mt-1 text-[12px] leading-[1.55] font-medium text-primary-tint-ink opacity-80">
                물품 목록이나 상세 화면에는 절대 보이지 않고, 배정이 끝나면 다시 가려집니다.
              </div>
            </div>
          </div>

          <div className="flex justify-center px-5 pt-4">
            <button
              type="button"
              onClick={() => setMode('openchat')}
              className="cursor-pointer text-[14px] font-bold text-accent underline underline-offset-[3px]"
            >
              오픈채팅방 링크로 등록할게요
            </button>
          </div>
        </>
      )}

      <div className="flex-1" />

      <BottomActionBar>
        {saveError && (
          <p role="alert" className="mb-2.5 text-center text-[13px] font-semibold text-terracotta">
            {saveError}
          </p>
        )}
        <PrimaryButton
          label="저장하기"
          disabled={!canSave}
          loading={saving}
          loadingLabel="저장 중…"
          onClick={handleSave}
        />
      </BottomActionBar>
    </Screen>
  )
}
