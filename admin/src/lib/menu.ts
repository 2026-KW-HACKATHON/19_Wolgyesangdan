/** 사이드바 메뉴이자 페이지 목록. 상단 바 제목·부제도 여기서 가져온다 */
export interface MenuItem {
  path: string
  label: string
  /** 상단 바 부제 */
  subtitle: string
  /** Material Symbols 아이콘 이름 */
  icon: string
}

export const MENU: MenuItem[] = [
  { path: '/', label: '대시보드', subtitle: '오늘 확인할 일', icon: 'space_dashboard' },
  { path: '/verifications', label: '인증 서류', subtitle: '신입생 · 기초수급자 우선배정 서류', icon: 'fact_check' },
  { path: '/campaigns', label: '캠페인', subtitle: '기간 · 거점 설정', icon: 'campaign' },
  { path: '/inquiries', label: '문의', subtitle: '회원 문의 확인과 답변', icon: 'forum' },
  { path: '/items', label: '물품 관리', subtitle: '등록된 물품 확인 · 숨기기', icon: 'inventory_2' },
]
