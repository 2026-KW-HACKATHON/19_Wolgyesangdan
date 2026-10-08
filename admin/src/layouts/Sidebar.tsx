import { NavLink } from 'react-router-dom'
import MaterialIcon from '../components/MaterialIcon'
import { MENU } from '../lib/menu'

interface SidebarProps {
  nickname: string
  /** 메뉴별 처리할 건수 (경로 → 건수). 0이거나 없으면 배지를 숨긴다 — 대시보드 요약 API(#216) 연동 후 채운다 */
  counts?: Partial<Record<string, number>>
  onLogout: () => void
}

/** 왼쪽 사이드바 224px — 로고 · 메뉴 · 운영자 이름 · 로그아웃 */
export default function Sidebar({ nickname, counts = {}, onLogout }: SidebarProps) {
  return (
    <aside className="flex w-56 flex-none flex-col bg-admin-sidebar px-3.5 pt-5 pb-4">
      <div className="flex items-center gap-2 px-2.5 pb-6">
        <span className="font-hand text-[28px] leading-none font-bold text-screen">월계장터</span>
        <span className="rounded-md bg-primary-soft px-1.5 py-0.5 text-[11px] font-bold text-admin-sidebar">관리자</span>
      </div>

      <nav aria-label="관리자 메뉴" className="flex flex-col gap-0.5">
        {MENU.map((item) => {
          const count = counts[item.path] ?? 0
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                `flex h-[42px] items-center gap-2.5 rounded-[10px] px-3 text-[14px] ${
                  isActive ? 'bg-admin-sidebar-active font-bold text-surface' : 'font-medium text-primary-soft'
                }`
              }
            >
              <MaterialIcon name={item.icon} size={20} />
              <span className="flex-1">{item.label}</span>
              {count > 0 && (
                <span className="flex size-5 items-center justify-center rounded-full bg-terracotta text-[11px] font-bold text-surface">
                  {count}
                </span>
              )}
            </NavLink>
          )
        })}
      </nav>

      <div className="mt-auto flex items-center gap-2.5 border-t border-white/15 px-2 pt-4">
        <span className="flex size-8 items-center justify-center rounded-full bg-primary text-[13px] font-bold text-surface">
          {nickname.slice(0, 1)}
        </span>
        <span className="flex-1 truncate text-[13px] font-bold text-screen">{nickname}</span>
        <button type="button" onClick={onLogout} aria-label="로그아웃" className="text-primary-soft hover:text-surface">
          <MaterialIcon name="logout" size={20} />
        </button>
      </div>
    </aside>
  )
}
