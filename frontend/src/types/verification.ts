export type UploadStatus = 'uploading' | 'done' | 'error'

export interface UploadedFile {
  id: string
  name: string
  size: number
  status: UploadStatus
  /** 로컬 미리보기/전송용 오브젝트 URL. 서버 업로드 연동 전까지는 클라이언트에만 존재합니다. */
  url?: string
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
