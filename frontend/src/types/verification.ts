export type UploadStatus = 'uploading' | 'done' | 'error'

export interface UploadedFile {
  id: string
  name: string
  size: number
  status: UploadStatus
  /** 로컬 미리보기용 오브젝트 URL (URL.createObjectURL). 다 쓰면 URL.revokeObjectURL로 정리해야 한다. */
  url?: string
  /** 서버(S3)에 올라간 사진 주소. 물품 등록 사진은 업로드가 끝나면(status: 'done') 채워진다. */
  imageUrl?: string
}

/** GPS 동네 인증(1b) 화면 상태 */
export type LocationStatus =
  | 'locating'
  | 'inside'
  | 'outside'
  | 'denied'
  /** 오차 100m 초과 */
  | 'inaccurate'
  /** 시간 초과·기기 오류 등으로 위치를 못 찾음 */
  | 'unavailable'

export interface Coords {
  lat: number
  lng: number
  /** 오차 반경 (m) */
  accuracy: number
}

export interface LocationCheckResult {
  inside: boolean
  /** 예: "서울 노원구 월계1동" */
  dongName: string
}

/** 우선배정 인증 유형 — 신입생 / 기초수급자 */
export type PriorityType = 'freshman' | 'basic'

/** 접수 완료 화면에 router state로 넘기는 제출 결과 */
export interface PrioritySubmitMeta {
  type: PriorityType
  submittedAt: Date
  docCount: number
}

// ── 백엔드 /verifications API ──

/** NEIGHBORHOOD는 나눔 신청 자격(GPS 동네 인증), FRESHMAN·LOW_INCOME은 우선배정 가산점용 */
export type VerificationType = 'NEIGHBORHOOD' | 'FRESHMAN' | 'LOW_INCOME'
export type PriorityVerificationType = Exclude<VerificationType, 'NEIGHBORHOOD'>

/** 우선배정 인증 서류 종류. 합격증·학생증은 FRESHMAN, 수급자 증명서는 LOW_INCOME 전용 */
export type DocumentType = 'ADMISSION_LETTER' | 'STUDENT_ID_CARD' | 'RECIPIENT_CERTIFICATE'

/** 조회 시점 기준 상태. 만료일이 지난 승인은 서버가 EXPIRED로 내려준다. */
export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'

/** 우선배정 서류로 받는 형식 — 사진 또는 PDF 한 파일 */
export type DocumentContentType =
  | 'image/jpeg'
  | 'image/png'
  | 'image/webp'
  | 'image/heic'
  | 'image/heif'
  | 'application/pdf'

/** POST /verifications/documents/upload-url 응답. 서류는 공개 URL이 없어 저장 위치(fileKey)만 받는다 */
export interface DocumentUploadUrlResponse {
  uploadUrl: string
  fileKey: string
}

/** POST /verifications 요청 — 서류 파일은 먼저 S3에 올리고 fileKey만 보낸다 */
export interface VerificationCreateRequest {
  verificationType: PriorityVerificationType
  documentType: DocumentType
  /** 서류 업로드 URL 발급 응답의 fileKey (S3 위치, 공개 URL 아님) */
  fileKey: string
  /** 서류에 적힌 이름 — 관리자가 서류와 대조한다 */
  applicantName: string
}

/** POST /verifications, POST /verifications/neighborhood 응답 */
export interface VerificationCreateResponse {
  id: number
  verificationType: VerificationType
  status: VerificationStatus
  submittedAt: string
}

/** GET /verifications/me 응답의 한 건. 신청한 적 없는 유형은 배열에서 빠진다. */
export interface MyVerification {
  verificationType: VerificationType
  /** 동네 인증은 서류가 없어서 null */
  documentType: DocumentType | null
  status: VerificationStatus
  submittedAt: string
  reviewedAt: string | null
  /** REJECTED일 때만 */
  rejectionReason: string | null
  /** APPROVED일 때만 */
  expiresAt: string | null
}
