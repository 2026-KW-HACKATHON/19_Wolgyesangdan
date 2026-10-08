export type Role = 'USER' | 'ADMIN'

/** GET /users/me 응답 중 관리자 웹에서 쓰는 부분 */
export interface MyInfo {
  id: number
  nickname: string
  role: Role
}
