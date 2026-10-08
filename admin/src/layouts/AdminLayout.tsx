import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { clearTokens } from '../lib/authStorage'
import { MENU } from '../lib/menu'
import Sidebar from './Sidebar'
import TopBar from './TopBar'

/** 관리자 화면 틀 — 사이드바 + 상단 바 + 본문(패딩 28px 32px, 최대 1200px) */
export default function AdminLayout({ nickname }: { nickname: string }) {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  // 하위 경로(/verifications/3 등)도 그 메뉴로 본다
  const page =
    MENU.find((item) => item.path !== '/' && (pathname === item.path || pathname.startsWith(`${item.path}/`))) ??
    MENU[0]

  const handleLogout = () => {
    clearTokens()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex h-full">
      <Sidebar nickname={nickname} onLogout={handleLogout} />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar title={page.label} subtitle={page.subtitle} />
        <main className="flex-1 overflow-y-auto px-8 py-7">
          <div className="mx-auto max-w-[1200px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
