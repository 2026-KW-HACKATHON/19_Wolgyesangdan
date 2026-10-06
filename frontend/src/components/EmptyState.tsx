// 목록이 비었을 때 보여주는 화면 — 매듭이 풀린 빈 보따리 그림 + 제목 + 안내 + (선택) 버튼.
// 색은 로그인 화면 그림(ExchangeScene)의 보따리·땅 색과 맞춘다.

const GROUND = '#D9CFAF'
const CLOTH = '#C9764A'
const CLOTH_DARK = '#A85E36'
const CLOTH_LIGHT = '#E4A574'
const BREEZE = '#CFC5A9'

/** 바닥에 펼쳐진 빈 보자기. 위쪽 매듭이 풀려 양 끝이 늘어져 있다. */
function EmptyBundle() {
  return (
    <svg viewBox="0 0 120 96" width={112} height={90} aria-hidden="true" className="flex-none">
      {/* 바닥 그림자 */}
      <ellipse cx="60" cy="80" rx="46" ry="7" fill={GROUND} opacity="0.7" />
      {/* 펼쳐진 보자기 (위에서 비스듬히 본 마름모) */}
      <path
        d="M60 32 Q63 32 66 34 L99 53 Q103 56 99 59 L66 77 Q60 80 54 77 L21 59 Q17 56 21 53 L54 34 Q57 32 60 32 Z"
        fill={CLOTH}
        stroke={CLOTH_DARK}
        strokeWidth="1.2"
        strokeLinejoin="round"
      />
      {/* 보자기 무늬 — 안쪽 점선 테두리와 가운데 꽃무늬 */}
      <path
        d="M60 40 L88 56 L60 71 L32 56 Z"
        fill="none"
        stroke={CLOTH_LIGHT}
        strokeWidth="1.3"
        strokeDasharray="3 3"
        strokeLinejoin="round"
      />
      <circle cx="60" cy="56" r="3" fill={CLOTH_LIGHT} />
      <circle cx="53" cy="56" r="1.4" fill={CLOTH_LIGHT} />
      <circle cx="67" cy="56" r="1.4" fill={CLOTH_LIGHT} />
      <circle cx="60" cy="51.5" r="1.4" fill={CLOTH_LIGHT} />
      <circle cx="60" cy="60.5" r="1.4" fill={CLOTH_LIGHT} />
      {/* 풀린 매듭 — 위 모서리에서 양쪽으로 늘어진 끈 */}
      <path
        d="M58 33 C52 26 45 26 42 30 C40 33 44 35 48 33"
        fill="none"
        stroke={CLOTH_DARK}
        strokeWidth="3.2"
        strokeLinecap="round"
      />
      <path
        d="M62 33 C68 24 77 23 80 27 C82 30 78 33 74 31"
        fill="none"
        stroke={CLOTH}
        strokeWidth="3.2"
        strokeLinecap="round"
      />
      {/* 조용한 바람 */}
      <path d="M14 30 Q20 27 26 30 M94 18 Q100 15 106 18 M100 26 Q104 24 108 26" fill="none" stroke={BREEZE} strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

interface EmptyStateProps {
  title: string
  description: string
  action?: { label: string; onClick: () => void }
}

export default function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center px-8 py-10 text-center">
      <EmptyBundle />
      <p className="mt-3 font-hand text-[22px] leading-tight font-bold text-label">{title}</p>
      <p className="mt-1.5 text-[13px] leading-[1.55] font-medium text-label-alt">{description}</p>
      {action && (
        <button
          type="button"
          onClick={action.onClick}
          className="mt-4 rounded-xl bg-primary px-5 py-2.5 text-[14px] font-bold text-screen"
        >
          {action.label}
        </button>
      )}
    </div>
  )
}
