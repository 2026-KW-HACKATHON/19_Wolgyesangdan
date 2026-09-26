import MaterialIcon from './icons/MaterialIcon'

interface NoticeBoxProps {
  icon: string
  text: string
  variant: 'green' | 'amber'
}

const VARIANT_CLASS = {
  green: { box: 'bg-primary-tint', icon: 'text-accent', text: 'text-primary-tint-ink' },
  amber: { box: 'bg-amber-tint', icon: 'text-terracotta-ink', text: 'text-terracotta-deep' },
}

/** 안내 박스 — 개인정보 안내(green), 검토 소요 안내(amber) 등에 사용. */
export default function NoticeBox({ icon, text, variant }: NoticeBoxProps) {
  const cls = VARIANT_CLASS[variant]

  return (
    <div className={`mx-5 mt-4.5 flex gap-2 rounded-2xl px-4 py-3.5 ${cls.box}`}>
      <MaterialIcon name={icon} size={18} className={`mt-px flex-none ${cls.icon}`} />
      <span className={`text-[13px] font-medium leading-[1.55] ${cls.text}`}>{text}</span>
    </div>
  )
}
