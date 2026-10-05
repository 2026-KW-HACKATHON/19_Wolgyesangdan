import { useEffect, useState, type CSSProperties } from 'react'

// 로그인 화면 그림 — 마주 선 상단(보부상) 차림의 두 사람이 물건을 손에서 손으로 맞바꿀 때마다 뒤의 나무가 한 단계씩 자란다.
// 한 바퀴 = 6박자: 0 작은 나무 → 1~4 교환할 때마다 성장(4번째에 다 자람) → 5 다 자란 채로 한 박자 쉼 → 처음으로.
// 박자는 state로 세고, 움직임 자체는 CSS transition·animation(index.css의 exchange-*)이 맡는다.
//
// 교환 한 번(1.6초)의 흐름 — 몸·팔·손·물건이 같은 구간(0→35→55→100%)으로 움직여서 물건이 손에서 떨어지지 않는다.
//   0~35%   한 걸음 다가서며 팔을 내밀어 두 물건이 가운데에서 나란히 만난다
//   35~55%  물건이 서로의 손으로 넘어간다 (상자가 단지 위로 살짝 들려 지나간다)
//   55~100% 받은 물건을 든 채 팔을 거두고 제자리로 물러난다

const BEAT_MS = 2200
const BEATS = 6
const HOLD_BEAT = 5
const MAX_STAGE = 4
/** 교환 횟수(0~4)별 나무 크기 */
const TREE_SCALE = [0.26, 0.46, 0.66, 0.84, 1]

/** 다가서는 한 걸음 (px) */
const STEP = 8
/** 팔을 내밀 때 손이 더 나가는 거리 (px). 팔 길이 18 → 30 */
const REACH = 12
const ARM_STRETCH = (18 + REACH) / 18
/** 물건이 가는 길 (자기 손 기준 x): 제자리 → 가운데에서 만남 → 넘어가는 중 → 상대 손 → 상대의 제자리 */
const CARRY = [0, STEP + REACH, 28, 36, 56]

const GROUND = '#D9CFAF'
const TRUNK = '#8A5A34'
const LEAF_1 = '#7E9A55'
const LEAF_2 = '#6B8A44'
const LEAF_3 = '#5E7F35'
const FRUIT_A = '#E4A574'
const FRUIT_B = '#B9603A'
// 상단 사람 — 패랭이(목화송이 달린 갓), 흰 저고리·바지에 색 배자, 등에 봇짐, 짚신
const SKIN = '#F6DDBF'
const CHEEK = '#F0A58E'
const FACE = '#3F2C1C'
const HAIR = '#3A2A1E'
const COTTON = '#FBF3E0'
const COTTON_EDGE = '#D8CCAF'
const STRAW = '#DDBE7C'
const STRAW_EDGE = '#A9853F'
const VEST_L = '#5E7F35'
const VEST_R = '#B9603A'
const BUNDLE_L = '#C9764A'
const BUNDLE_R = '#7E9A55'
const BOX = '#B9603A'
const BOX_LID = '#8A4A28'
const JAR = '#E8B24F'
const JAR_LID = '#A96B33'

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

interface MerchantProps {
  /** 교환 중이면 박자마다 애니메이션을 새로 재생한다 */
  beat: number
  exchanging: boolean
  /** 배자(조끼) 색 */
  vest: string
  /** 등에 멘 봇짐 보자기 색 */
  bundle: string
}

/**
 * 오른쪽을 보고 선 상단 사람 (발끝 y=128, 몸 중심 x=72). 오른쪽 사람은 이 그림을 좌우로 뒤집어 쓴다.
 * 머리를 크게 그린 2.5등신. 소매 넓은 팔을 내밀어 손(92,98) 위에 물건을 올려 든다.
 */
function Merchant({ beat, exchanging, vest, bundle }: MerchantProps) {
  const once = (name: string) => (exchanging ? name : undefined)

  return (
    // key를 박자로 주면 교환할 때마다 다시 그려져서 한 번짜리 애니메이션이 새로 재생된다
    <g key={beat} className={once('animate-exchange-step')} style={{ '--step': `${STEP}px` } as CSSProperties}>
      {/* 봇짐 — 등 뒤에 멘 보자기 꾸러미와 매듭 */}
      <ellipse cx="56.5" cy="103" rx="7.2" ry="8.2" fill={bundle} />
      <path d="M53.5 96 L50.5 91.5 L56 94 L58.5 90 L59.5 95.5 Z" fill={bundle} />

      {/* 바지 · 대님 · 짚신 */}
      <rect x="62.5" y="113" width="8.6" height="13" rx="3.6" fill={COTTON} stroke={COTTON_EDGE} strokeWidth="0.8" />
      <rect x="72.9" y="113" width="8.6" height="13" rx="3.6" fill={COTTON} stroke={COTTON_EDGE} strokeWidth="0.8" />
      <rect x="63" y="122.6" width="7.6" height="1.6" rx="0.8" fill={vest} />
      <rect x="73.4" y="122.6" width="7.6" height="1.6" rx="0.8" fill={vest} />
      <ellipse cx="67.6" cy="126.8" rx="5.4" ry="2.3" fill={STRAW} stroke={STRAW_EDGE} strokeWidth="0.8" />
      <ellipse cx="78" cy="126.8" rx="5.4" ry="2.3" fill={STRAW} stroke={STRAW_EDGE} strokeWidth="0.8" />

      {/* 배자(조끼) 입은 몸통 · 흰 깃 · 봇짐 끈 */}
      <path d="M64.5 96.5 Q72 93.5 79.5 96.5 L83 116 Q72 118.5 61 116 Z" fill={vest} />
      <path d="M68.6 95.4 L74.2 104.6 M76.6 95.4 L72.6 102" fill="none" stroke={COTTON} strokeWidth="2.4" strokeLinecap="round" />
      <path d="M62.6 99 L81.6 110.5" fill="none" stroke={bundle} strokeWidth="2.2" strokeLinecap="round" />

      {/* 머리 · 머리카락 · 얼굴 (보는 쪽으로 살짝 치우침) */}
      <circle cx="72" cy="83" r="11" fill={SKIN} />
      <path d="M61 83 A11 11 0 0 1 83 83 Q79 76.5 72 77 Q65 77.5 61 83 Z" fill={HAIR} />
      <ellipse cx="69.2" cy="88.8" rx="2.5" ry="1.6" fill={CHEEK} opacity="0.85" />
      <ellipse cx="81" cy="88.6" rx="2.2" ry="1.6" fill={CHEEK} opacity="0.85" />
      <circle cx="72.6" cy="85" r="1.45" fill={FACE} />
      <circle cx="79.4" cy="85" r="1.45" fill={FACE} />
      <path d="M74.2 88.2 Q76 90.4 77.8 88.2" fill="none" stroke={FACE} strokeWidth="1.2" strokeLinecap="round" />

      {/* 패랭이 — 둥근 모자집, 끈, 넓은 챙, 목화송이 두 개 */}
      <path d="M63.4 75.4 C63.4 65 67 61.4 72 61.4 C77 61.4 80.6 65 80.6 75.4 Z" fill={STRAW} stroke={STRAW_EDGE} strokeWidth="0.9" />
      <path d="M63.6 72.6 Q72 74.6 80.4 72.6" fill="none" stroke={FACE} strokeWidth="1.5" strokeLinecap="round" />
      <ellipse cx="72" cy="75.6" rx="16.5" ry="3.5" fill={STRAW} stroke={STRAW_EDGE} strokeWidth="0.9" />
      <circle cx="66.6" cy="63.4" r="3.1" fill={COTTON} stroke={COTTON_EDGE} strokeWidth="0.8" />
      <circle cx="77.4" cy="63.4" r="3.1" fill={COTTON} stroke={COTTON_EDGE} strokeWidth="0.8" />

      {/* 팔 — 넓은 흰 소매가 어깨(74,98)를 기준으로 앞으로 늘어나고, 손은 그만큼 따라 나간다 */}
      <rect
        x="74"
        y="94.4"
        width="18"
        height="7.2"
        rx="3.4"
        fill={COTTON}
        stroke={COTTON_EDGE}
        strokeWidth="0.8"
        vectorEffect="non-scaling-stroke"
        className={once('animate-exchange-reach')}
        style={{ transformOrigin: '74px 98px', '--stretch': ARM_STRETCH } as CSSProperties}
      />
      <circle
        cx="92.6"
        cy="98"
        r="3.3"
        fill={SKIN}
        className={once('animate-exchange-step')}
        style={{ '--step': `${REACH}px` } as CSSProperties}
      />
    </g>
  )
}

interface ExchangeSceneProps {
  className?: string
}

export default function ExchangeScene({ className }: ExchangeSceneProps) {
  // "동작 줄이기" 설정이면 다 자란 나무로 멈춰 둔다
  const [beat, setBeat] = useState(() => (prefersReducedMotion() ? HOLD_BEAT : 0))

  useEffect(() => {
    if (prefersReducedMotion()) return
    const id = window.setInterval(() => setBeat((b) => (b + 1) % BEATS), BEAT_MS)
    return () => window.clearInterval(id)
  }, [])

  const stage = Math.min(beat, MAX_STAGE)
  // 이번 박자에 교환이 일어나는가 (1~4박)
  const exchanging = beat >= 1 && beat <= MAX_STAGE
  // 홀수 번째 교환은 물건이 상대에게 가고, 짝수 번째는 되돌아온다. 4번을 마치면 제자리다
  const path = beat % 2 === 1 ? CARRY : [...CARRY].reverse()

  // 물건을 건네받고 물러날 때 나무가 자란다. 처음으로 돌아갈 때는 바로 줄어든다
  const treeStyle: CSSProperties = {
    transformOrigin: '120px 128px',
    transform: `scale(${TREE_SCALE[stage]})`,
    transition: `transform 0.8s cubic-bezier(0.3, 1.4, 0.5, 1) ${exchanging ? '1.1s' : '0s'}`,
  }
  const fruitStyle: CSSProperties = {
    opacity: stage === MAX_STAGE ? 1 : 0,
    transition: stage === MAX_STAGE ? 'opacity 0.4s ease-out 1.7s' : 'opacity 0.2s',
  }
  /** 물건 하나의 이동. direction은 상대 쪽 방향(+1 오른쪽, -1 왼쪽), lift는 넘어갈 때 들리는 높이 */
  const carry = (direction: 1 | -1, lift: number) =>
    ({
      '--x0': `${path[0] * direction}px`,
      '--x1': `${path[1] * direction}px`,
      '--x2': `${path[2] * direction}px`,
      '--x3': `${path[3] * direction}px`,
      '--x4': `${path[4] * direction}px`,
      '--lift': `${lift}px`,
    }) as CSSProperties
  const carryClass = exchanging ? 'animate-exchange-carry' : undefined

  return (
    <svg
      width={240}
      height={150}
      viewBox="0 0 240 150"
      role="img"
      aria-label="상단 차림의 두 사람이 물건을 주고받을 때마다 나무가 자라는 그림"
      className={className}
    >
      <ellipse cx="120" cy="131" rx="106" ry="9" fill={GROUND} />

      {/* 나무 — 두 사람 뒤, 밑동(120,128)을 기준으로 커진다 */}
      <g style={treeStyle}>
        <rect x="113" y="80" width="14" height="48" rx="5" fill={TRUNK} />
        <circle cx="103" cy="62" r="29" fill={LEAF_1} />
        <circle cx="137" cy="60" r="29" fill={LEAF_2} />
        <circle cx="120" cy="40" r="31" fill={LEAF_3} />
        <g style={fruitStyle}>
          <circle cx="104" cy="46" r="5" fill={FRUIT_A} />
          <circle cx="128" cy="28" r="5" fill={FRUIT_B} />
          <circle cx="140" cy="52" r="5" fill={FRUIT_A} />
          <circle cx="118" cy="58" r="5" fill={FRUIT_B} />
        </g>
      </g>

      {/* 왼쪽 사람, 오른쪽 사람(좌우로 뒤집음) */}
      <Merchant beat={beat} exchanging={exchanging} vest={VEST_L} bundle={BUNDLE_L} />
      <g transform="translate(240 0) scale(-1 1)">
        <Merchant beat={beat} exchanging={exchanging} vest={VEST_R} bundle={BUNDLE_R} />
      </g>

      {/* 왼쪽 사람 손(92,98) 위의 상자 — 넘어갈 때 단지 위로 살짝 들린다 */}
      <g key={`box-${beat}`} className={carryClass} style={carry(1, -13)}>
        <rect x="86" y="86" width="12" height="9.5" rx="1.5" fill={BOX} />
        <rect x="85" y="84" width="14" height="3.5" rx="1.5" fill={BOX_LID} />
      </g>

      {/* 오른쪽 사람 손(148,98) 위의 단지 */}
      <g key={`jar-${beat}`} className={carryClass} style={carry(-1, 0)}>
        <rect x="142.5" y="85.5" width="11" height="10" rx="4" fill={JAR} />
        <rect x="145" y="83" width="6" height="3.5" rx="1.5" fill={JAR_LID} />
      </g>
    </svg>
  )
}
