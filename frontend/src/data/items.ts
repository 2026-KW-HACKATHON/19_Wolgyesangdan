export type TradeMethod = 'DIRECT' | 'CAMPAIGN'
export type ItemStatus = 'OPEN' | 'ASSIGNING' | 'DONE'
export type CategoryGroup = '가구' | '가전' | '주방' | '생활' | '기타'
export type Condition = '거의 새것' | '상태 좋음' | '사용감 있음'
export type TransportDifficulty = '쉬움' | '보통' | '어려움'

export type Item = {
  id: string
  name: string
  categoryGroup: CategoryGroup
  category: string
  condition: Condition
  icon: string
  carbonKg: number
  tradeMethods: TradeMethod[]
  status: ItemStatus
  deadlineLabel: string
  description: string
  usagePeriod: string
  defect: string
  workingStatus: string
  size: string
  transportDifficulty: TransportDifficulty
  applicantCount: number
  maxApplicants: number
  ownerName: string
  ownerCompletedCount: number
}

export const STATUS_LABEL: Record<ItemStatus, string> = {
  OPEN: '신청 가능',
  ASSIGNING: '배정 중',
  DONE: '거래 완료',
}

export const TRADE_METHOD_LABEL: Record<TradeMethod, string> = {
  DIRECT: '직거래',
  CAMPAIGN: '거점 수령',
}

export const CONDITION_RANK: Record<Condition, number> = {
  '거의 새것': 0,
  '상태 좋음': 1,
  '사용감 있음': 2,
}

export const ITEMS: Item[] = [
  {
    id: '1',
    name: '전자레인지',
    categoryGroup: '가전',
    category: '생활가전',
    condition: '상태 좋음',
    icon: 'microwave',
    carbonKg: 24,
    tradeMethods: ['CAMPAIGN', 'DIRECT'],
    status: 'OPEN',
    deadlineLabel: '10.2까지',
    description: '이사 때문에 내놓아요. 내부 청소 완료했고 회전판까지 그대로 있어요. 작동에 문제 없습니다.',
    usagePeriod: '2년 사용',
    defect: '없음',
    workingStatus: '정상 작동',
    size: '48 × 36 × 28cm',
    transportDifficulty: '보통',
    applicantCount: 3,
    maxApplicants: 5,
    ownerName: '월계1동 이웃',
    ownerCompletedCount: 3,
  },
  {
    id: '2',
    name: '1인용 책상',
    categoryGroup: '가구',
    category: '책상·의자',
    condition: '사용감 있음',
    icon: 'desk',
    carbonKg: 41,
    tradeMethods: ['DIRECT'],
    status: 'OPEN',
    deadlineLabel: '9.30까지',
    description: '3년 정도 쓴 책상이에요. 모서리에 약간 사용감 있지만 흔들림 없이 튼튼합니다.',
    usagePeriod: '3년 사용',
    defect: '모서리 사용감',
    workingStatus: '해당 없음',
    size: '100 × 50 × 72cm',
    transportDifficulty: '어려움',
    applicantCount: 5,
    maxApplicants: 5,
    ownerName: '월계1동 이웃',
    ownerCompletedCount: 1,
  },
  {
    id: '3',
    name: '전기포트 1.7L',
    categoryGroup: '주방',
    category: '주방가전',
    condition: '거의 새것',
    icon: 'kettle',
    carbonKg: 9,
    tradeMethods: ['CAMPAIGN'],
    status: 'OPEN',
    deadlineLabel: '10.4까지',
    description: '선물 받았는데 이미 있어서 거의 안 썼어요. 박스도 있습니다.',
    usagePeriod: '1개월 사용',
    defect: '없음',
    workingStatus: '정상 작동',
    size: '22 × 15 × 20cm',
    transportDifficulty: '쉬움',
    applicantCount: 1,
    maxApplicants: 5,
    ownerName: '월계1동 이웃',
    ownerCompletedCount: 5,
  },
  {
    id: '4',
    name: '책장 3단',
    categoryGroup: '가구',
    category: '수납',
    condition: '상태 좋음',
    icon: 'shelves',
    carbonKg: 33,
    tradeMethods: ['DIRECT'],
    status: 'ASSIGNING',
    deadlineLabel: '9.28까지',
    description: '원목 느낌 책장이에요. 무겁지 않아서 혼자서도 옮길 수 있어요.',
    usagePeriod: '1년 사용',
    defect: '없음',
    workingStatus: '해당 없음',
    size: '60 × 24 × 90cm',
    transportDifficulty: '보통',
    applicantCount: 5,
    maxApplicants: 5,
    ownerName: '월계1동 이웃',
    ownerCompletedCount: 2,
  },
  {
    id: '5',
    name: '스탠드 조명',
    categoryGroup: '생활',
    category: '조명',
    condition: '상태 좋음',
    icon: 'floor_lamp',
    carbonKg: 6,
    tradeMethods: ['DIRECT'],
    status: 'OPEN',
    deadlineLabel: '10.5까지',
    description: '은은한 조명이라 침실용으로 좋아요. LED 전구 포함입니다.',
    usagePeriod: '6개월 사용',
    defect: '없음',
    workingStatus: '정상 작동',
    size: '25 × 25 × 140cm',
    transportDifficulty: '쉬움',
    applicantCount: 0,
    maxApplicants: 5,
    ownerName: '월계1동 이웃',
    ownerCompletedCount: 0,
  },
  {
    id: '6',
    name: '접이식 의자 2개',
    categoryGroup: '가구',
    category: '책상·의자',
    condition: '사용감 있음',
    icon: 'chair',
    carbonKg: 18,
    tradeMethods: ['DIRECT'],
    status: 'ASSIGNING',
    deadlineLabel: '9.26까지',
    description: '캠핑용으로 샀는데 안 쓰게 됐어요. 2개 같이 드려요.',
    usagePeriod: '2년 사용',
    defect: '다리 부분 스크래치',
    workingStatus: '해당 없음',
    size: '48 × 45 × 80cm',
    transportDifficulty: '쉬움',
    applicantCount: 4,
    maxApplicants: 5,
    ownerName: '월계1동 이웃',
    ownerCompletedCount: 1,
  },
  {
    id: '7',
    name: '빨래 건조대',
    categoryGroup: '생활',
    category: '세탁',
    condition: '거의 새것',
    icon: 'local_laundry_service',
    carbonKg: 12,
    tradeMethods: ['DIRECT'],
    status: 'OPEN',
    deadlineLabel: '10.1까지',
    description: '접이식이라 보관도 편해요. 몇 번 안 썼습니다.',
    usagePeriod: '2개월 사용',
    defect: '없음',
    workingStatus: '해당 없음',
    size: '60 × 60 × 90cm (접었을 때)',
    transportDifficulty: '쉬움',
    applicantCount: 2,
    maxApplicants: 5,
    ownerName: '월계1동 이웃',
    ownerCompletedCount: 4,
  },
]
