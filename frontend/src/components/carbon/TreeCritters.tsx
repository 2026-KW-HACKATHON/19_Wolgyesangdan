import type { CSSProperties, ReactNode } from 'react'

// 동네 나무 장면에 레벨마다 놓는 작은 그림 (해 · 사슴벌레 · 새 · 사슴, 숲에는 다람쥐 · 토끼까지).
// 아이콘 폰트에 없는 것들이라 장면 색에 맞춰 SVG로 그린다. 위치는 TreeScene이 348×236 무대 좌표로 준다.
// 움직임은 index.css의 critter-* 애니메이션을 쓰고, "동작 줄이기" 설정이면 멈춘 그림으로 둔다.

const SUN = '#E8B24F'
const BEETLE = '#4A3524'
const BEETLE_LINE = '#7A5A3E'
const BIRD = '#8A4A28'
const BIRD_WING = '#6E3A1E'
const BIRD_BELLY = '#E4A574'
const DEER = '#B07A4A'
const DEER_SPOT = '#E4C9A0'
const ANTLER = '#8A5A34'
const SQUIRREL = '#C9764A'
const SQUIRREL_TAIL = '#A8552F'
const ACORN = '#C99A5B'
const RABBIT = '#A89880'
const RABBIT_FOOT = '#8F8069'
const BELLY = '#F2D9B8'
const DARK = '#3F2C1C'
const CREAM = '#FFFDF6'

interface CritterProps {
  /** 무대 안 위치 (left/top/bottom 등) */
  style: CSSProperties
}

interface SvgProps extends CritterProps {
  width: number
  height: number
  viewBox: string
  children: ReactNode
}

function Svg({ width, height, viewBox, style, children }: SvgProps) {
  return (
    // 뛰어오르는 토끼처럼 그림 밖으로 나가는 움직임이 잘리지 않게 overflow-visible
    <svg width={width} height={height} viewBox={viewBox} className="absolute overflow-visible" style={style}>
      {children}
    </svg>
  )
}

/**
 * 한 점을 축으로 까딱이는 부분의 스타일. origin은 그 부분(자기 테두리 상자) 기준 위치이고,
 * rot만큼 돌았다가 돌아온다. animate-critter-sway / animate-critter-nod 와 함께 쓴다.
 */
function pivot(origin: string, rot: string, duration?: string): CSSProperties {
  return {
    transformBox: 'fill-box',
    transformOrigin: origin,
    animationDuration: duration,
    '--critter-rot': rot,
  } as CSSProperties
}

/** Lv.1 새싹 — 해 */
export function Sun({ style }: CritterProps) {
  const rays = [0, 45, 90, 135, 180, 225, 270, 315]
  return (
    <Svg width={46} height={46} viewBox="0 0 48 48" style={style}>
      <circle cx="24" cy="24" r="10" fill={SUN} />
      {rays.map((deg) => (
        <line
          key={deg}
          x1="24"
          y1="8.5"
          x2="24"
          y2="4"
          stroke={SUN}
          strokeWidth="3"
          strokeLinecap="round"
          transform={`rotate(${deg} 24 24)`}
        />
      ))}
    </Svg>
  )
}

/** Lv.2 묘목 — 사슴벌레 (위에서 본 모습). 큰턱을 벌렸다 오므렸다 한다 */
export function StagBeetle({ style }: CritterProps) {
  return (
    <Svg width={24} height={30} viewBox="0 0 32 40" style={style}>
      <g fill="none" stroke={BEETLE} strokeWidth="2.2" strokeLinecap="round">
        {/* 큰턱 — 머리에 붙은 쪽(아래 안쪽 끝)을 축으로 바깥으로 벌어진다 */}
        <g className="animate-critter-sway motion-reduce:animate-none" style={pivot('100% 100%', '-18deg', '1.1s')}>
          <path d="M13 10 C7 8 6 3 11 1" />
          <path d="M9 5.5 L11.5 6.5" />
        </g>
        <g className="animate-critter-sway motion-reduce:animate-none" style={pivot('0% 100%', '18deg', '1.1s')}>
          <path d="M19 10 C25 8 26 3 21 1" />
          <path d="M23 5.5 L20.5 6.5" />
        </g>
        {/* 다리 */}
        <path d="M9 17 L4 14" />
        <path d="M23 17 L28 14" />
        <path d="M8 26 L2 26" />
        <path d="M24 26 L30 26" />
        <path d="M9 32 L4 37" />
        <path d="M23 32 L28 37" />
      </g>
      <rect x="11" y="9" width="10" height="6" rx="3" fill={BEETLE} />
      <rect x="9" y="14" width="14" height="7" rx="3" fill={BEETLE} />
      <ellipse cx="16" cy="29" rx="8" ry="10" fill={BEETLE} />
      <line x1="16" y1="21" x2="16" y2="38" stroke={BEETLE_LINE} strokeWidth="1.2" strokeLinecap="round" />
    </Svg>
  )
}

/** Lv.3 나무 — 나무 꼭대기에 앉은 새. 날개를 조금씩 펄럭인다 */
export function Bird({ style }: CritterProps) {
  return (
    <Svg width={30} height={24} viewBox="0 0 34 27" style={style}>
      <g stroke={ANTLER} strokeWidth="1.5" strokeLinecap="round">
        <line x1="14" y1="21" x2="14" y2="26" />
        <line x1="18" y1="21" x2="18" y2="26" />
      </g>
      <polygon points="7,12 0,8 1,15 6,18" fill={BIRD_WING} />
      <ellipse cx="15" cy="15" rx="10" ry="7.5" fill={BIRD} />
      <ellipse cx="17" cy="17.5" rx="6" ry="4.2" fill={BIRD_BELLY} />
      <circle cx="23" cy="9" r="5.5" fill={BIRD} />
      {/* 날개 — 어깨(오른쪽 끝)를 축으로 날개 끝이 위로 올라갔다 내려온다 */}
      <path
        d="M8 12 Q14 10 19 14 Q14 19 8 16 Z"
        fill={BIRD_WING}
        className="animate-critter-sway motion-reduce:animate-none"
        style={pivot('100% 50%', '32deg', '0.55s')}
      />
      <polygon points="28,7.5 33,9.5 28,11.5" fill={SUN} />
      <circle cx="24.5" cy="8" r="1.1" fill={CREAM} />
    </Svg>
  )
}

/** Lv.4 큰나무 — 나무 아래 사슴. 고개를 위아래로 까딱인다 */
export function Deer({ style }: CritterProps) {
  return (
    <Svg width={46} height={46} viewBox="0 0 48 48" style={style}>
      {/* 다리 (뒤쪽 두 개는 살짝 어둡게) */}
      <rect x="15.5" y="30" width="3.2" height="16" rx="1.6" fill={ANTLER} />
      <rect x="27" y="30" width="3.2" height="16" rx="1.6" fill={ANTLER} />
      <rect x="11" y="29" width="3.2" height="17" rx="1.6" fill={DEER} />
      <rect x="31.5" y="29" width="3.2" height="17" rx="1.6" fill={DEER} />
      {/* 꼬리 · 몸 */}
      <ellipse cx="8.5" cy="22.5" rx="2.2" ry="3" fill={CREAM} />
      <ellipse cx="22" cy="27" rx="13" ry="7.5" fill={DEER} />
      {/* 목 · 뿔 · 귀 · 머리 — 목이 몸에 붙은 자리(왼쪽 아래)를 축으로 숙였다 든다 */}
      <g className="animate-critter-nod motion-reduce:animate-none" style={pivot('0% 88%', '13deg')}>
        <g fill="none" stroke={ANTLER} strokeWidth="1.6" strokeLinecap="round">
          <path d="M37 8 C36 4 37 2 39 0.8" />
          <path d="M36.8 5 L34 2.8" />
          <path d="M40 8 C40.5 5 42 3.2 44.5 2.2" />
          <path d="M41.5 4.6 L42.4 1.6" />
        </g>
        <path d="M30 25 L35 12 L40 14 L35 30 Z" fill={DEER} stroke={DEER} strokeWidth="2" strokeLinejoin="round" />
        <ellipse cx="34.5" cy="8.5" rx="1.8" ry="3.2" fill={DEER} transform="rotate(-40 34.5 8.5)" />
        <ellipse cx="39.5" cy="12" rx="5.5" ry="4" fill={DEER} transform="rotate(15 39.5 12)" />
        <circle cx="44.3" cy="13.6" r="1.2" fill={DARK} />
        <circle cx="39.6" cy="10.8" r="0.9" fill={DARK} />
      </g>
      {/* 등의 점 */}
      <circle cx="17" cy="24" r="1" fill={DEER_SPOT} />
      <circle cx="22" cy="22.5" r="1" fill={DEER_SPOT} />
      <circle cx="26" cy="25" r="1" fill={DEER_SPOT} />
    </Svg>
  )
}

/** Lv.5 숲 — 다람쥐 (왼쪽을 보고 앉은 모습). 도토리를 입에 가져가 갉아 먹는다 */
export function Squirrel({ style }: CritterProps) {
  return (
    <Svg width={26} height={26} viewBox="0 0 32 32" style={style}>
      <path
        d="M22 28 C30 28 31 18 27 12 C25 8 20 9 20 13 C20 16 23 16 23 19 C23 23 21 26 22 28 Z"
        fill={SQUIRREL_TAIL}
      />
      <ellipse cx="12" cy="29" rx="4" ry="1.6" fill={SQUIRREL_TAIL} />
      <ellipse cx="15" cy="21" rx="6.5" ry="8" fill={SQUIRREL} />
      <ellipse cx="12.5" cy="22" rx="3" ry="5" fill={BELLY} />
      {/* 머리 — 갉을 때마다 살짝 끄덕인다 */}
      <g className="animate-critter-sway motion-reduce:animate-none" style={pivot('70% 100%', '-7deg', '0.4s')}>
        <ellipse cx="13" cy="6" rx="1.6" ry="2.6" fill={SQUIRREL} />
        <circle cx="11" cy="11" r="5" fill={SQUIRREL} />
        <circle cx="9.5" cy="10.5" r="0.9" fill={DARK} />
        <circle cx="6.2" cy="12" r="0.8" fill={DARK} />
      </g>
      {/* 앞발과 도토리 — 입 쪽으로 올렸다 내린다 */}
      <g className="animate-critter-nibble motion-reduce:animate-none">
        <path d="M11.5 18.5 L7.5 17.5" fill="none" stroke={SQUIRREL} strokeWidth="2" strokeLinecap="round" />
        <ellipse cx="5.8" cy="17" rx="1.9" ry="2.3" fill={ACORN} />
        <path d="M3.7 16.3 Q5.8 13.6 7.9 16.3 Z" fill={ANTLER} />
      </g>
    </Svg>
  )
}

/** Lv.5 숲 — 토끼 (오른쪽을 보고 앉은 모습). 제자리에서 껑충껑충 뛴다 */
export function Rabbit({ style }: CritterProps) {
  return (
    <Svg width={24} height={26} viewBox="0 0 30 32" style={style}>
      <g className="animate-critter-hop motion-reduce:animate-none">
        <ellipse cx="19.5" cy="6.5" rx="2" ry="6" fill={RABBIT_FOOT} transform="rotate(-8 19.5 6.5)" />
        <ellipse cx="23" cy="7" rx="2" ry="6" fill={RABBIT} transform="rotate(12 23 7)" />
        <circle cx="4.5" cy="23" r="2.8" fill={CREAM} />
        <ellipse cx="16" cy="30" rx="5" ry="1.8" fill={RABBIT_FOOT} />
        <ellipse cx="13" cy="22" rx="9" ry="8" fill={RABBIT} />
        <circle cx="21" cy="15" r="5.5" fill={RABBIT} />
        <ellipse cx="21" cy="28.5" rx="2" ry="2.5" fill={RABBIT} />
        <circle cx="22.5" cy="14" r="0.9" fill={DARK} />
        <circle cx="26" cy="16" r="0.8" fill={BIRD_BELLY} />
      </g>
    </Svg>
  )
}
