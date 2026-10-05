import type { DocumentType, PriorityType, PriorityVerificationType } from '../types/verification'

export interface PriorityOption {
  /** API의 인증 유형 */
  verificationType: PriorityVerificationType
  title: string
  description: string
  /** 1c 카드에 보이는 서류 칩 = 1d·1e 서류 종류 선택지 */
  docTypes: string[]
  icon: string
  /** 아이콘 타일 배경·글리프 색 Tailwind 클래스 */
  tileClassName: string
}

export const PRIORITY_OPTIONS: Record<PriorityType, PriorityOption> = {
  freshman: {
    verificationType: 'FRESHMAN',
    title: '신입생 인증',
    description: '올해 광운대학교에 입학한 신입생',
    docTypes: ['합격증', '학생증'],
    icon: 'school',
    tileClassName: 'bg-clay-tint text-clay-ink',
  },
  basic: {
    verificationType: 'LOW_INCOME',
    title: '기초수급자 인증',
    description: '국민기초생활보장 수급 가구',
    docTypes: ['수급자 증명서'],
    icon: 'volunteer_activism',
    tileClassName: 'bg-primary-tint text-accent',
  },
}

export const PRIORITY_TYPES = Object.keys(PRIORITY_OPTIONS) as PriorityType[]

/** API 서류 종류 ↔ 화면 이름. 이름은 PRIORITY_OPTIONS의 docTypes와 같아야 한다 */
export const DOCUMENT_TYPE_LABEL: Record<DocumentType, string> = {
  ADMISSION_LETTER: '합격증',
  STUDENT_ID_CARD: '학생증',
  RECIPIENT_CERTIFICATE: '수급자 증명서',
}

/** 화면에서 고른 서류 이름을 API 서류 종류로 바꾼다 */
export function toDocumentType(label: string) {
  const entry = Object.entries(DOCUMENT_TYPE_LABEL).find(([, name]) => name === label)
  return entry ? (entry[0] as DocumentType) : null
}
