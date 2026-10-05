// 백엔드 /users API 타입

/** GET /users/me 응답 */
export interface MyInfo {
  id: number
  nickname: string
  email: string | null
  contactType: 'PHONE' | 'OPENCHAT' | null
  phone: string | null
  openchatLink: string | null
  /** 가입 일시 (서버 로컬 시각, 시간대 없음) */
  createdAt: string
}
