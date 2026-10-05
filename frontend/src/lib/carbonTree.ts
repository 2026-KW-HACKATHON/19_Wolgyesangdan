// 동네 나무 계산 규칙 (README_탄소절감리포트 "계산 규칙").
// 레벨 구간, 열매 1개당 물품 수는 기획 확정 전 가정값이라 상수로 모아 둔다.

export type TreeLevelNo = 1 | 2 | 3 | 4 | 5

export interface TreeLevel {
  lv: TreeLevelNo
  name: string
  /** 이 레벨이 시작되는 누적 탄소 절감량 (kg CO₂e) */
  min: number
  /** 레벨에 도달했을 때 보여주는 문구 */
  message: string
  /** 장면에 그릴 수 있는 최대 열매 수 */
  maxFruits: number
}

export const TREE_LEVELS: readonly TreeLevel[] = [
  { lv: 1, name: '새싹', min: 0, message: '첫 나눔으로 새싹이 났어요', maxFruits: 0 },
  { lv: 2, name: '묘목', min: 2000, message: '뿌리를 내리기 시작했어요', maxFruits: 0 },
  { lv: 3, name: '나무', min: 5000, message: '동네에 그늘이 생겼어요', maxFruits: 2 },
  { lv: 4, name: '큰나무', min: 10000, message: '열매가 주렁주렁 열렸어요', maxFruits: 5 },
  { lv: 5, name: '숲', min: 20000, message: '월계1동에 숲이 생겼어요', maxFruits: 9 },
]

/** 소나무 1그루 연간 흡수량 (kg CO₂e) — 국립산림과학원 표준탄소흡수량, 30년생 소나무 기준 */
export const KG_PER_TREE = 6.6
/** 열매 1개 = 물품 100개 */
export const ITEMS_PER_FRUIT = 100

export interface TreeProgress {
  level: TreeLevel
  /** 다음 레벨. 숲(Lv.5)이면 null */
  next: TreeLevel | null
  /** 다음 레벨까지 남은 kg. 숲이면 0 */
  remainKg: number
  /** 현재 레벨 구간 안에서의 진행률 0~1. 숲이면 1 */
  progress: number
  fruits: number
}

export function treeProgress(carbonReductionKg: number, reusedCount: number): TreeProgress {
  let level = TREE_LEVELS[0]
  for (const l of TREE_LEVELS) {
    if (carbonReductionKg >= l.min) level = l
  }
  const next = TREE_LEVELS[level.lv] ?? null
  return {
    level,
    next,
    remainKg: next ? next.min - carbonReductionKg : 0,
    progress: next ? (carbonReductionKg - level.min) / (next.min - level.min) : 1,
    fruits: Math.min(Math.floor(reusedCount / ITEMS_PER_FRUIT), level.maxFruits),
  }
}

/** 탄소 절감량을 소나무 그루 수로 환산 */
export function treesFromKg(kg: number) {
  return Math.round(kg / KG_PER_TREE)
}

/** 받침 유무에 따라 '이'/'가'를 붙인다. 예: 숲이, 나무가 */
export function withSubjectParticle(word: string) {
  const code = word.charCodeAt(word.length - 1) - 0xac00
  const hasFinal = code >= 0 && code <= 11171 && code % 28 !== 0
  return `${word}${hasFinal ? '이' : '가'}`
}
