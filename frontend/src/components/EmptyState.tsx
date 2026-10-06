import { Bird } from './carbon/TreeCritters'

// 목록이 비었을 때 보여주는 화면 — 묶인 빨간 보따리 위에서 새가 날개짓하는 그림 + 제목 + 안내 + (선택) 버튼.
// 새는 탄소 리포트 동네 나무의 새(TreeCritters.Bird)를 그대로 쓴다.

const GROUND = '#D9CFAF'
const CLOTH = '#B8433A'
const CLOTH_DARK = '#8E2F28'
const CLOTH_LIGHT = '#D86A55'

/** 위에서 매듭지은 둥근 보자기 보따리 */
function Bundle() {
  return (
    <svg viewBox="0 0 140 120" width={128} height={110} aria-hidden="true" className="block">
      {/* 바닥 그림자 */}
      <ellipse cx="70" cy="113" rx="50" ry="6" fill={GROUND} opacity="0.75" />
      {/* 몸통 — 아래가 넓게 퍼진 둥근 보따리 */}
      <path
        d="M70 40 C50 40 30 52 23 72 C17 90 24 106 44 111 C58 114 82 114 96 111 C116 106 123 90 117 72 C110 52 90 40 70 40 Z"
        fill={CLOTH}
        stroke={CLOTH_DARK}
        strokeWidth="1.4"
      />
      {/* 왼쪽 위 빛 */}
      <path d="M36 70 C40 58 50 50 60 47 C52 56 46 68 44 84 C40 82 36 77 36 70 Z" fill={CLOTH_LIGHT} opacity="0.55" />
      {/* 매듭으로 모이는 주름 */}
      <g fill="none" stroke={CLOTH_DARK} strokeWidth="1.4" strokeLinecap="round" opacity="0.55">
        <path d="M64 44 C52 58 40 76 34 98" />
        <path d="M69 46 C66 66 62 88 60 109" />
        <path d="M75 45 C84 62 94 80 102 101" />
        <path d="M79 43 C92 52 104 62 112 76" />
      </g>
      {/* 매듭 아래 오므린 목 */}
      <path d="M61 42 C64 37 76 37 79 42 C76 46 64 46 61 42 Z" fill={CLOTH_DARK} />
      {/* 매듭 귀 두 개 */}
      <path d="M66 36 C58 30 50 22 54 16 C58 11 66 22 69 33 Z" fill={CLOTH} stroke={CLOTH_DARK} strokeWidth="1.3" strokeLinejoin="round" />
      <path d="M74 35 C80 26 88 16 93 20 C98 25 86 33 76 38 Z" fill={CLOTH} stroke={CLOTH_DARK} strokeWidth="1.3" strokeLinejoin="round" />
      {/* 매듭 */}
      <ellipse cx="70" cy="36" rx="7" ry="5.5" fill={CLOTH} stroke={CLOTH_DARK} strokeWidth="1.3" />
      {/* 매듭이 조여진 주름 */}
      <path d="M67 32 L69.5 39 M73 32 L71 39" fill="none" stroke={CLOTH_DARK} strokeWidth="1.1" strokeLinecap="round" />
    </svg>
  )
}

/** 보따리 + 왼쪽 어깨에 앉아 날개짓하며 몸을 들썩이는 새 */
function BundleWithBird() {
  return (
    <div className="relative" style={{ width: 128, height: 110 }} aria-hidden="true">
      <Bundle />
      {/* 위치·크기는 바깥 div, 들썩임은 안쪽 div가 맡는다 (transform이 겹치지 않게) */}
      <div className="absolute" style={{ left: 22, top: 22, width: 30, height: 24, transform: 'scale(1.3)' }}>
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
