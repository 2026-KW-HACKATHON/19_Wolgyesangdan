export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

/** POST /auth/admin/login 응답 (카카오 로그인 응답과 같은 형식) */
export interface LoginResponse extends AuthTokens {
  isNewUser: boolean
  user: {
    id: number
    nickname: string
    email: string | null
  }
}
