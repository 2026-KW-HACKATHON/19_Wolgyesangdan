// 백엔드 /admin/verifications API 타입

/** 관리자 화면에서 다루는 우선배정 유형 (동네 인증은 심사가 없어 나오지 않는다) */
export type VerificationType = 'FRESHMAN' | 'LOW_INCOME'

export type DocumentType = 'ADMISSION_LETTER' | 'STUDENT_ID_CARD' | 'RECIPIENT_CERTIFICATE'

/** 조회 시점 기준 상태. 만료일이 지난 승인은 EXPIRED로 내려온다 */
export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'

/** GET /admin/verifications 의 한 줄. applicantName은 검토 후 30일이 지나 서류를 지웠으면 null */
export interface AdminVerificationSummary {
  id: number
  applicantName: string | null
  nickname: string
  verificationType: VerificationType
  documentType: DocumentType
  submittedAt: string
  status: VerificationStatus
}

/** GET /admin/verifications/{id}. 승인·반려 응답도 같은 형태 */
export interface AdminVerificationDetail extends AdminVerificationSummary {
  neighborhoodVerified: boolean
  /** false면 서류 파일이 없다 (검토 후 30일이 지나 삭제 등) */
  hasDocument: boolean
  rejectionReason: string | null
  reviewedAt: string | null
  reviewerNickname: string | null
  expiresAt: string | null
}

/** GET /admin/verifications/{id}/file — 5분 동안만 열리는 서류 주소 */
export interface AdminVerificationFile {
  url: string
  contentType: string
  expiresAt: string
}
