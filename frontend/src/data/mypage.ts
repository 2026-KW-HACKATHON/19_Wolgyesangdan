// TODO: 백엔드 연결 후 GET /me, /me/items 응답으로 대체

export const PROFILE = {
  name: '정하늘',
  initial: '정',
  dong: '월계1동',
  joinedMonths: 2,
  provider: '카카오',
}

export const MY_RECORD = {
  co2eTotalKg: 83,
  reusedCount: 3,
  byItem: [
    { title: '전자레인지', kg: 24 },
    { title: '책상', kg: 41 },
    { title: '의자', kg: 18 },
  ],
}

export interface RegisteredItem {
  id: string
  title: string
  icon: string
  iconClassName: string
  status: 'OPEN' | 'DONE'
  meta: string
}

export const REGISTERED_ITEMS: RegisteredItem[] = [
  {
    id: '2',
    title: '1인용 책상',
    icon: 'desk',
    iconClassName: 'bg-clay-tint text-clay-ink',
    status: 'OPEN',
    meta: '신청자 5명 · 9.30 전달 예정',
  },
  {
    id: 'rice-cooker',
    title: '전기밥솥 3인용',
    icon: 'rice_bowl',
    iconClassName: 'bg-sunken text-label-alt',
    status: 'DONE',
    meta: '신청자 5명 · 9.14 거점 전달',
  },
]
