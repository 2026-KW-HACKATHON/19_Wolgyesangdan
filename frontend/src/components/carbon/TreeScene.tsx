import type { CSSProperties, ReactNode } from 'react'
import type { TreeLevelNo } from '../../lib/carbonTree'
import MaterialIcon from '../icons/MaterialIcon'

// 시안 5d의 CSS 도형을 그대로 옮긴 장면. 좌표는 카드 안쪽 폭 348 × 높이 236 기준이고,
// 카드 폭이 달라도 가운데 정렬된 348px 무대 위에 그려서 비율이 깨지지 않게 한다.

const LEAF_1 = '#7E9A55'
const LEAF_2 = '#6B8A44'
const LEAF_3 = '#5E7F35'
const LEAF_FAR = '#AFC48C'
const LEAF_PALE = '#9FB878'
const TRUNK = '#8A5A34'
const TRUNK_FAR = '#9C7451'
const MOUND = '#C4B78F'
const FRUIT_A = '#E4A574'
const FRUIT_B = '#B9603A'

type Shape =
  | { kind: 'box'; style: CSSProperties }
  | { kind: 'icon'; name: string; size: number; color: string; style: CSSProperties }

const box = (style: CSSProperties): Shape => ({ kind: 'box', style })
const round = (left: number, top: number, width: number, height: number, background: string) =>
  box({ left, top, width, height, background, borderRadius: 999 })
const trunk = (left: number, width: number, height: number, radius: string, background = TRUNK, bottom = 34) =>
  box({ left, bottom, width, height, borderRadius: radius, background })
const icon = (name: string, size: number, color: string, style: CSSProperties): Shape => ({
  kind: 'icon',
  name,
  size,
  color,
  style,
})

/** 열매 자리. 앞에서부터 fruits 개수만큼 그린다. */
type FruitSlot = { left: number; top: number; color: string }

/** 레이어 순서: 하늘 → far(뒷줄 나무) → 땅 → near(덤불·줄기·잎 원) → 열매 → after (칩은 부모가 그린다) */
interface Scene {
  far: Shape[]
  near: Shape[]
  fruits: FruitSlot[]
  after: Shape[]
}

const BIG_TREE: Shape[] = [
  trunk(161, 28, 84, '9px 9px 4px 4px'),
  round(85, 46, 110, 100, LEAF_1),
  round(156, 40, 112, 104, LEAF_2),
  round(118, 20, 118, 96, LEAF_3),
]

const BIG_TREE_FRUITS: FruitSlot[] = [
  { left: 120, top: 62, color: FRUIT_A },
  { left: 184, top: 44, color: FRUIT_B },
  { left: 216, top: 92, color: FRUIT_A },
  { left: 156, top: 104, color: FRUIT_B },
  { left: 102, top: 108, color: FRUIT_A },
]

const SCENES: Record<TreeLevelNo, Scene> = {
  1: {
    far: [],
    near: [
      box({ left: 173, bottom: 36, width: 4, height: 34, borderRadius: 2, background: LEAF_1 }),
      box({
        left: 149,
        bottom: 62,
        width: 28,
        height: 16,
        borderRadius: '16px 0 16px 0',
        background: LEAF_1,
        transform: 'rotate(-14deg)',
      }),
      box({
        left: 177,
        bottom: 68,
        width: 30,
        height: 17,
        borderRadius: '0 17px 0 17px',
        background: LEAF_3,
        transform: 'rotate(12deg)',
      }),
      box({ left: 150, bottom: 34, width: 52, height: 8, borderRadius: 999, background: MOUND }),
    ],
    fruits: [],
    after: [],
  },
  2: {
    far: [],
    near: [
      trunk(170, 10, 58, '3px 3px 4px 4px'),
      round(128, 72, 94, 84, LEAF_1),
      round(150, 58, 64, 60, LEAF_2),
    ],
    fruits: [],
    after: [icon('eco', 24, LEAF_PALE, { left: 60, bottom: 38 })],
  },
  3: {
    far: [],
    near: [
      trunk(165, 20, 72, '7px 7px 4px 4px'),
      round(104, 52, 100, 94, LEAF_1),
      round(162, 46, 100, 96, LEAF_2),
      round(130, 28, 96, 84, LEAF_3),
    ],
    fruits: [
      { left: 150, top: 70, color: FRUIT_A },
      { left: 206, top: 98, color: FRUIT_B },
    ],
    after: [icon('potted_plant', 26, LEAF_1, { left: 50, bottom: 38 })],
  },
  4: {
    far: [],
    near: BIG_TREE,
    fruits: BIG_TREE_FRUITS,
    // 양옆 작은 나무 2그루 (대칭)
    after: [
      trunk(47, 12, 40, '4px'),
      round(21, 110, 64, 58, LEAF_1),
      round(30, 98, 44, 42, LEAF_2),
      round(40, 126, 10, 10, FRUIT_A),
      trunk(291, 12, 40, '4px'),
      round(265, 110, 64, 58, LEAF_1),
      round(276, 98, 44, 42, LEAF_2),
      round(300, 126, 10, 10, FRUIT_A),
    ],
  },
  5: {
    far: [
      // 뒷줄 연한 나무 2
      trunk(96, 10, 44, '4px', TRUNK_FAR, 40),
      round(70, 88, 62, 58, LEAF_FAR),
      trunk(252, 10, 44, '4px', TRUNK_FAR, 40),
      round(226, 88, 62, 58, LEAF_FAR),
      // 가장자리 나무 2
      trunk(14, 9, 34, '4px', TRUNK, 38),
      round(-8, 118, 54, 50, LEAF_2),
      trunk(328, 9, 34, '4px', TRUNK, 38),
      round(306, 118, 54, 50, LEAF_2),
    ],
    near: [
      // 덤불 4
      box({ left: 66, bottom: 30, width: 44, height: 26, borderRadius: 999, background: LEAF_1 }),
      box({ left: 236, bottom: 30, width: 48, height: 26, borderRadius: 999, background: LEAF_1 }),
      box({ left: 150, bottom: 24, width: 22, height: 16, borderRadius: 999, background: LEAF_PALE }),
      box({ left: 196, bottom: 24, width: 22, height: 16, borderRadius: 999, background: LEAF_PALE }),
      // 앞줄 작은 나무 2
      trunk(42, 14, 48, '5px 5px 4px 4px'),
      round(12, 92, 76, 72, LEAF_1),
      round(26, 78, 54, 52, LEAF_2),
      trunk(292, 14, 48, '5px 5px 4px 4px'),
      round(262, 92, 76, 72, LEAF_1),
      round(276, 78, 54, 52, LEAF_2),
      ...BIG_TREE,
    ],
    fruits: [
      ...BIG_TREE_FRUITS,
      { left: 146, top: 34, color: FRUIT_A },
      { left: 236, top: 64, color: FRUIT_B },
      { left: 40, top: 104, color: FRUIT_A },
      { left: 296, top: 108, color: FRUIT_B },
    ],
    after: [
      icon('eco', 22, LEAF_PALE, { left: 100, bottom: 36 }),
      icon('eco', 22, LEAF_PALE, { right: 104, bottom: 36 }),
    ],
  },
}

function renderShape(shape: Shape, key: number) {
  if (shape.kind === 'icon') {
    return (
      <MaterialIcon
        key={key}
        name={shape.name}
        size={shape.size}
        color={shape.color}
        style={{ position: 'absolute', ...shape.style }}
      />
    )
  }
  return <div key={key} className="absolute" style={shape.style} />
}

interface TreeSceneProps {
  level: TreeLevelNo
  fruits: number
}

/** 하늘·땅·나무 장면. 부모가 position:relative, 높이 236px, overflow hidden인 영역을 준다. */
export default function TreeScene({ level, fruits }: TreeSceneProps) {
  const scene = SCENES[level]

  return (
    <>
      <Stage>{scene.far.map(renderShape)}</Stage>
      {/* 땅은 348px 무대가 아니라 카드 폭 전체에 깐다 */}
      <div className="absolute inset-x-0 bottom-0 h-[46px] rounded-[50%_50%_0_0/22px_22px_0_0] bg-[#D9CFAF]" />
      <Stage>
        {scene.near.map(renderShape)}
        {scene.fruits.slice(0, fruits).map((f, i) => (
          <span
            key={`fruit-${i}`}
            className="absolute size-[13px] rounded-full"
            style={{ left: f.left, top: f.top, background: f.color }}
          />
        ))}
        {scene.after.map(renderShape)}
      </Stage>
    </>
  )
}

function Stage({ children }: { children: ReactNode }) {
  return (
    <div aria-hidden="true" className="absolute inset-y-0 left-1/2 w-[348px] -translate-x-1/2">
      {children}
    </div>
  )
}
