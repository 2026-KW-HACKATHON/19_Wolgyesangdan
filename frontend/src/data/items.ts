export type TradeMethod = 'DIRECT' | 'CAMPAIGN'
export type ItemStatus = 'OPEN' | 'ASSIGNING' | 'DONE'

export type Item = {
  id: string
  name: string
  category: string
  condition: string
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

export const ITEMS: Item[] = [
  {
    id: '1',
    name: '전자레인지',
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
    category: '수납',
    condition: '상태 좋음',
    icon: 'shelves',
    carbonKg: 33,
    tradeMethods: ['DIRECT'],
    status: 'ASSIGNING',
    deadlineLabel: '9.28까지',
  },
]
