export type VerificationMethod = 'resident' | 'student'

export type UploadStatus = 'uploading' | 'done' | 'error'

export interface UploadedFile {
  id: string
  name: string
  size: number
  status: UploadStatus
  /** 로컬 미리보기/전송용 오브젝트 URL. 서버 업로드 연동 전까지는 클라이언트에만 존재합니다. */
  url?: string
}

export interface ResidentVerificationForm {
  docType: string
  files: UploadedFile[]
  name: string
  address: {
    roadAddress: string
    detail: string
    isWolgye: boolean
  }
  agreed: boolean
}

export interface StudentVerificationForm {
  docType: string
  files: UploadedFile[]
  school: string
  department: string
  studentId: string
  agreed: boolean
}

export interface VerificationSubmitMeta {
  type: VerificationMethod
  label: string
  submittedAt: Date
  docCount: number
}
