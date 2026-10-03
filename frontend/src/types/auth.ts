export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

/** POST /auth/kakao 응답 */
export interface LoginResponse extends AuthTokens {
  isNewUser: boolean
  user: {
    id: number
    nickname: string
    email: string | null
  }
}
