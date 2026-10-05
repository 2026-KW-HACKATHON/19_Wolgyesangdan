// TODO: 백엔드 연결 후 GET /me, /me/items 응답으로 대체

export const PROFILE = {
  name: '정하늘',
  initial: '정',
  dong: '월계1동',
  joinedMonths: 2,
  provider: '카카오',
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

export type AppliedStatus = 'WAITING' | 'ASSIGNED' | 'DONE'

export interface AppliedItem {
  id: string
  title: string
  icon: string
  iconClassName: string
  status: AppliedStatus
  /** 신청 당시 대기 순번 (WAITING/ASSIGNED) */
  waitlistNo?: number
  waitlistCount?: number
  meta: string
  /** 배정된 경우에만 등록자가 고른 연락 수단 */
  contact?: { type: 'openchat' | 'phone'; value: string }
  co2eKg?: number
}

export const APPLIED_ITEMS: AppliedItem[] = [
  {
    id: 'microwave',
    title: '전자레인지',
    icon: 'microwave',
    iconClassName: 'bg-primary-tint text-accent',
    status: 'WAITING',
    waitlistNo: 2,
    waitlistCount: 5,
    meta: '거점 수령 · 10.2까지',
  },
  {
    id: 'chair',
    title: '접이식 의자 2개',
    icon: 'chair',
    iconClassName: 'bg-clay-tint text-clay-ink',
    status: 'ASSIGNED',
    waitlistNo: 1,
    meta: '직거래 · 9.24 17:00 약속',
    contact: { type: 'openchat', value: 'open.kakao.com/o/gW1kXyZ' },
  },
  {
    id: 'shelf',
    title: '책장 3단',
    icon: 'shelves',
    iconClassName: 'bg-clay-tint text-clay-ink',
    status: 'WAITING',
    waitlistNo: 5,
    waitlistCount: 5,
    meta: '직거래 · 9.28까지',
  },
  {
    id: 'rice-cooker',
    title: '전기밥솥 3인용',
    icon: 'rice_bowl',
    iconClassName: 'bg-sunken text-label-alt',
    status: 'DONE',
    meta: '9.14 거점 수령',
    co2eKg: 27,
  },
]
