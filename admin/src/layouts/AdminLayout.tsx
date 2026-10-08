import { useEffect, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { getAdminSummary } from '../api/summary'
import { clearTokens } from '../lib/authStorage'
import { MENU } from '../lib/menu'
import { SummaryContext, type SummaryContextValue } from '../lib/summaryContext'
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

  // 요약(GET /admin/summary)은 여기서 한 번 불러와 사이드바 배지 · 상단 바 캠페인 칩 · 대시보드가 같이 쓴다 (#217).
  // 메뉴를 옮길 때마다 다시 불러와서, 다른 화면에서 처리한 건수가 배지에 반영된다
  const [value, setValue] = useState<SummaryContextValue>({ summary: null, failed: false })

  useEffect(() => {
    let ignore = false
    getAdminSummary()
      .then((summary) => {
        if (!ignore) setValue({ summary, failed: false })
      })
      .catch(() => {
        // 못 불러와도 이전에 받은 값은 그대로 둔다 — 배지와 칩은 참고용이다
        if (!ignore) setValue((current) => ({ summary: current.summary, failed: true }))
      })
    return () => {
      ignore = true
    }
  }, [page.path])

  const handleLogout = () => {
    clearTokens()
    navigate('/login', { replace: true })
  }

  const { summary } = value
  const counts = summary
    ? { '/verifications': summary.pendingVerifications, '/inquiries': summary.openInquiries }
    : undefined

  return (
    <SummaryContext value={value}>
      <div className="flex h-full">
        <Sidebar nickname={nickname} counts={counts} onLogout={handleLogout} />
        <div className="flex min-w-0 flex-1 flex-col">
          <TopBar title={page.label} subtitle={page.subtitle} campaign={summary?.currentCampaign ?? null} />
          <main className="flex-1 overflow-y-auto px-8 py-7">
            <div className="mx-auto max-w-[1200px]">
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    </SummaryContext>
  )
}
