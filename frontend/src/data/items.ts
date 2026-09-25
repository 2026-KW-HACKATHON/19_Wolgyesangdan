export type TradeMethod = 'DIRECT' | 'CAMPAIGN'
export type ItemStatus = 'OPEN' | 'ASSIGNING' | 'DONE'
export type CategoryGroup = '가구' | '가전' | '주방' | '생활' | '기타'
export type Condition = '거의 새것' | '상태 좋음' | '사용감 있음'

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
  },
]
