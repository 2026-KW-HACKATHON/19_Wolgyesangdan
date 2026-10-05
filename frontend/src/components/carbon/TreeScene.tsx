import type { CSSProperties, ReactNode } from 'react'
import type { TreeLevelNo } from '../../lib/carbonTree'
import MaterialIcon from '../icons/MaterialIcon'
import { Bird, Deer, Rabbit, Squirrel, StagBeetle, Sun } from './TreeCritters'

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
  | { kind: 'box'; style: CSSProperties; className?: string }
  | { kind: 'icon'; name: string; size: number; color: string; style: CSSProperties }

const box = (style: CSSProperties, className?: string): Shape => ({ kind: 'box', style, className })

/**
 * 새싹 잎. 줄기에 붙은 모서리(origin)를 축으로 from ↔ to 각도 사이를 천천히 오르내린다.
 * "동작 줄이기" 설정이면 from 각도로 멈춰 있다.
 */
const sproutLeaf = (style: CSSProperties, origin: string, from: number, to: number, duration: number): Shape =>
  box(
    {
      ...style,
      transformOrigin: origin,
      transform: `rotate(${from}deg)`,
      animationDuration: `${duration}s`,
      '--sway-from': `${from}deg`,
      '--sway-to': `${to}deg`,
    } as CSSProperties,
    'animate-sprout-sway motion-reduce:animate-none',
  )
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
      // 줄기는 두 잎의 뾰족한 끝이 닿는 높이까지 올라온다
      box({ left: 173, bottom: 36, width: 4, height: 54, borderRadius: 2, background: LEAF_1 }),
      // 왼쪽 잎은 오른쪽 위 모서리, 오른쪽 잎은 왼쪽 위 모서리가 줄기에 붙어 있다
      sproutLeaf(
        { left: 148, bottom: 62, width: 28, height: 16, borderRadius: '16px 0 16px 0', background: LEAF_1 },
        '100% 0%',
        -8,
        -20,
        2.8,
      ),
      sproutLeaf(
        { left: 174, bottom: 68, width: 30, height: 17, borderRadius: '0 17px 0 17px', background: LEAF_3 },
        '0% 0%',
        6,
        18,
        3.3,
      ),
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
      trunk(96, 10, 56, '4px', TRUNK_FAR, 40),
      round(70, 88, 62, 58, LEAF_FAR),
      trunk(252, 10, 56, '4px', TRUNK_FAR, 40),
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
    after: [],
  },
}

/** 레벨마다 놓는 작은 그림. 해는 레벨 칩 아래, 사슴벌레는 땅 위, 새는 나무 꼭대기, 사슴은 큰 나무 아래. 숲에는 여럿이 모인다 */
const CRITTERS: Partial<Record<TreeLevelNo, ReactNode>> = {
  1: <Sun style={{ left: 30, top: 50 }} />,
  2: <StagBeetle style={{ left: 232, bottom: 16, transform: 'rotate(-24deg)' }} />,
  3: <Bird style={{ left: 184, top: 9 }} />,
  4: <Deer style={{ left: 98, bottom: 32 }} />,
  5: (
    <>
      {/* 큰 나무 꼭대기와 왼쪽 앞줄 나무 위의 새 (왼쪽 새는 바깥쪽을 보게 뒤집는다) */}
      <Bird style={{ left: 186, top: 1 }} />
      <Bird style={{ left: 37, top: 55, transform: 'scaleX(-1)' }} />
      {/* 큰 나무 아래 왼쪽에 사슴, 오른쪽에 토끼, 오른쪽 앞줄 나무 옆에 다람쥐 */}
      <Deer style={{ left: 112, bottom: 32 }} />
      <Rabbit style={{ left: 208, bottom: 34 }} />
      <Squirrel style={{ left: 312, bottom: 34 }} />
    </>
  ),
}

/** 흩날리는 잎. 나무 잎 원 안에서 출발해 dx·dy만큼 떨어지며 땅 근처에서 사라진다. duration·delay는 초 */
interface DriftingLeaf {
  left: number
  top: number
  dx: number
  dy: number
  duration: number
  delay: number
  color: string
}

// 큰 나무 + 양옆 작은 나무 (Lv.4·5 공통 — 두 레벨의 작은 나무 잎 원이 겹치는 자리)
const BIG_TREE_LEAVES: DriftingLeaf[] = [
  { left: 112, top: 70, dx: -46, dy: 112, duration: 6.5, delay: 0, color: LEAF_1 },
  { left: 150, top: 40, dx: 38, dy: 146, duration: 7.5, delay: 1.2, color: LEAF_3 },
  { left: 200, top: 56, dx: 56, dy: 128, duration: 6, delay: 2.6, color: LEAF_PALE },
  { left: 236, top: 96, dx: 44, dy: 90, duration: 5.5, delay: 0.8, color: LEAF_2 },
  { left: 176, top: 30, dx: -60, dy: 154, duration: 8, delay: 3.4, color: LEAF_PALE },
  { left: 96, top: 104, dx: -34, dy: 82, duration: 5.8, delay: 4.1, color: LEAF_3 },
  { left: 44, top: 100, dx: -22, dy: 86, duration: 6.8, delay: 2, color: LEAF_1 },
  { left: 300, top: 104, dx: 26, dy: 82, duration: 6.2, delay: 4.8, color: LEAF_2 },
]

// 숲에서만 — 뒷줄 연한 나무, 가장자리 나무, 앞줄 작은 나무에서 한 장씩 더
const FOREST_LEAVES: DriftingLeaf[] = [
  { left: 92, top: 104, dx: -28, dy: 72, duration: 6.4, delay: 1.6, color: LEAF_FAR },
  { left: 262, top: 100, dx: 30, dy: 76, duration: 7, delay: 3, color: LEAF_FAR },
  { left: 16, top: 130, dx: 20, dy: 54, duration: 5.6, delay: 0.5, color: LEAF_PALE },
  { left: 326, top: 132, dx: -22, dy: 52, duration: 6, delay: 3.8, color: LEAF_PALE },
  { left: 66, top: 118, dx: 24, dy: 66, duration: 6.6, delay: 5.2, color: LEAF_2 },
  { left: 276, top: 116, dx: -26, dy: 68, duration: 5.9, delay: 1, color: LEAF_1 },
]

/** 큰나무(Lv.4)부터 잎이 흩날리고, 숲(Lv.5)은 모든 나무에서 흩날린다 */
const DRIFTING_LEAVES: Partial<Record<TreeLevelNo, DriftingLeaf[]>> = {
  4: BIG_TREE_LEAVES,
  5: [...BIG_TREE_LEAVES, ...FOREST_LEAVES],
}

function DriftingLeaves({ leaves }: { leaves: DriftingLeaf[] }) {
  return leaves.map((leaf, i) => (
    <span
      key={`leaf-${i}`}
      // 시작 전(delay 동안)에는 안 보이게 opacity-0, 움직임 줄이기 설정이면 아예 그리지 않는다
      className="absolute h-[7px] w-3 animate-leaf-drift rounded-[7px_0_7px_0] opacity-0 motion-reduce:hidden"
      style={
        {
          left: leaf.left,
          top: leaf.top,
          background: leaf.color,
          animationDuration: `${leaf.duration}s`,
          animationDelay: `${leaf.delay}s`,
          '--leaf-dx': `${leaf.dx}px`,
          '--leaf-dy': `${leaf.dy}px`,
        } as CSSProperties
      }
    />
  ))
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
  return <div key={key} className={`absolute ${shape.className ?? ''}`} style={shape.style} />
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
        {CRITTERS[level]}
        <DriftingLeaves leaves={DRIFTING_LEAVES[level] ?? []} />
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
