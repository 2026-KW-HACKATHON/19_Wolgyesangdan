import MaterialIcon from './icons/MaterialIcon'

interface AgreementCheckboxProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
}

/** 접근성 있는 커스텀 체크박스 — 실제 input에 라벨이 연결되어 있습니다. */
export default function AgreementCheckbox({ checked, onChange, label }: AgreementCheckboxProps) {
  return (
    <label className="flex cursor-pointer items-start gap-2.5 px-5 pt-4.5">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="peer sr-only"
      />
      <span
        className={`mt-px flex size-[22px] flex-none items-center justify-center rounded-md border peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary ${
          checked ? 'border-primary bg-primary' : 'border-border-deep bg-surface'
        }`}
      >
        {checked && <MaterialIcon name="check" size={16} className="text-screen" />}
      </span>
      <span className="text-[13px] font-medium leading-[1.55] text-body">
        {label} <strong className="font-bold text-terracotta">(필수)</strong>
      </span>
    </label>
  )
}
