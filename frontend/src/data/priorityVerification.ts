import type { PriorityType } from '../types/verification'

export interface PriorityOption {
  title: string
  description: string
  /** 1c 카드에 보이는 서류 칩 = 1d·1e 서류 종류 선택지 */
  docTypes: string[]
  icon: string
  /** 아이콘 타일 배경·글리프 색 Tailwind 클래스 */
  tileClassName: string
}

export const PRIORITY_OPTIONS: Record<PriorityType, PriorityOption> = {
  freshman: {
    title: '신입생 인증',
    description: '올해 광운대학교에 입학한 신입생',
    docTypes: ['합격증', '학생증'],
    icon: 'school',
    tileClassName: 'bg-clay-tint text-clay-ink',
  },
  basic: {
    title: '기초수급자 인증',
    description: '국민기초생활보장 수급 가구',
    docTypes: ['수급자 증명서'],
    icon: 'volunteer_activism',
    tileClassName: 'bg-primary-tint text-accent',
  },
}

export const PRIORITY_TYPES = Object.keys(PRIORITY_OPTIONS) as PriorityType[]
