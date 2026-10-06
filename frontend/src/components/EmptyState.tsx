import { Bird } from './carbon/TreeCritters'

// 목록이 비었을 때 보여주는 화면 — 네모나게 묶인 빨간 보따리 위에서 새가 날개짓하는 그림 + 제목 + 안내 + (선택) 버튼.
// 새는 탄소 리포트 동네 나무의 새(TreeCritters.Bird)를 그대로 쓴다.

const GROUND = '#D9CFAF'
const CLOTH = '#B8433A'
const CLOTH_DARK = '#8E2F28'
const CLOTH_LIGHT = '#D86A55'
const CLOTH_TOP = '#C8503F'

/** 네모난 상자를 감싸 위에서 리본처럼 매듭지은 보자기 보따리 (비스듬히 위에서 본 모습) */
function Bundle() {
  return (
    <svg viewBox="0 0 140 120" width={128} height={110} aria-hidden="true" className="block">
      {/* 바닥 그림자 */}
      <ellipse cx="70" cy="110" rx="58" ry="5" fill={GROUND} opacity="0.75" />
      {/* 앞면 */}
      <path
        d="M16 60 L124 60 Q127 60 127 64 L126 100 Q126 106 120 106 L20 106 Q14 106 14 100 L13 64 Q13 60 16 60 Z"
        fill={CLOTH}
        stroke={CLOTH_DARK}
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
      {/* 윗면 */}
      <path
        d="M16 60 L28 41 Q30 38 34 38 L106 38 Q110 38 112 41 L124 60 Z"
        fill={CLOTH_TOP}
        stroke={CLOTH_DARK}
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
      {/* 아래 모서리를 접어 올린 자리 */}
      <path d="M14 88 Q20 100 34 106 L20 106 Q14 106 14 100 Z" fill={CLOTH_DARK} opacity="0.45" />
      <path d="M126 88 Q120 100 106 106 L120 106 Q126 106 126 100 Z" fill={CLOTH_DARK} opacity="0.45" />
      {/* 매듭으로 당겨진 주름 — 윗면은 매듭 쪽으로 모이고, 앞면은 위에서 아래로 흐른다 */}
      <g fill="none" stroke={CLOTH_DARK} strokeWidth="1.3" strokeLinecap="round" opacity="0.5">
        <path d="M66 48 C52 50 36 54 24 58" />
        <path d="M74 48 C88 50 104 54 116 58" />
        <path d="M64 46 C56 43 46 41 36 40" />
        <path d="M76 46 C84 43 94 41 104 40" />
        <path d="M22 64 C20 76 20 88 22 98" />
        <path d="M118 64 C120 76 120 88 118 98" />
        <path d="M40 62 C46 72 54 80 62 86" />
        <path d="M100 62 C94 72 86 80 78 86" />
      </g>
      {/* 앞면 위쪽 빛 */}
      <path d="M20 63 L120 63 L118 68 L22 68 Z" fill={CLOTH_LIGHT} opacity="0.35" />
      {/* 리본 매듭 — 옆으로 펼친 귀 두 장, 위로 솟은 귀 두 장, 앞으로 늘어진 끝 */}
      <g stroke={CLOTH_DARK} strokeWidth="1.3" strokeLinejoin="round">
        <path d="M66 48 C58 44 50 44 44 49 C51 54 59 54 66 51 Z" fill={CLOTH} />
        <path d="M74 48 C82 44 90 44 96 49 C89 54 81 54 74 51 Z" fill={CLOTH} />
        <path d="M67 45 C60 38 56 30 58 24 C64 27 68 36 69 44 Z" fill={CLOTH_LIGHT} />
        <path d="M73 45 C78 37 84 30 89 28 C88 35 82 41 74 47 Z" fill={CLOTH_LIGHT} />
        <path d="M70 51 C66 60 66 70 72 78 C76 70 76 60 72 51 Z" fill={CLOTH} />
        <ellipse cx="70" cy="48" rx="6" ry="4.6" fill={CLOTH} />
      </g>
      <path d="M67.5 45.5 L69 50.5 M72.5 45.5 L71 50.5" fill="none" stroke={CLOTH_DARK} strokeWidth="1" strokeLinecap="round" />
    </svg>
  )
}

/** 보따리 + 윗면 왼쪽에 앉아 날개짓하며 몸을 들썩이는 새 */
function BundleWithBird() {
  return (
    <div className="relative" style={{ width: 128, height: 110 }} aria-hidden="true">
      <Bundle />
      {/* 위치·크기는 바깥 div, 들썩임은 안쪽 div가 맡는다 (transform이 겹치지 않게) */}
      <div className="absolute" style={{ left: 22, top: 14, width: 30, height: 24, transform: 'scale(1.3)' }}>
        <div className="relative size-full animate-bird-bob motion-reduce:animate-none">
          <Bird style={{ left: 0, top: 0 }} />
        </div>
      </div>
    </div>
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
      <BundleWithBird />
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
