import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { isLoggedIn } from '../lib/authStorage'
import { loginPath } from '../lib/loginRedirect'
import LoginRequiredDialog from './LoginRequiredDialog'

type NavItem = {
  to: string
  label: string
  icon: string
  /** 비로그인이면 이동하지 않고 "로그인이 필요해요" 팝업을 띄울 때의 안내 문구 */
  loginRequired?: string
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/browse', label: '둘러보기', icon: 'explore' },
  {
    to: '/register',
    label: '등록',
    icon: 'add_box',
    loginRequired: '물품을 등록하려면 로그인해 주세요. 로그인하면 바로 등록 화면으로 이어져요.',
  },
  { to: '/carbon-report', label: '탄소절감 리포트', icon: 'park' },
  { to: '/mypage', label: '마이페이지', icon: 'person' },
]

export default function BottomNav() {
  const navigate = useNavigate()
  // 로그인이 필요한 탭을 비로그인으로 눌렀을 때 — 지금 화면 위에 팝업을 띄운다
  const [loginPrompt, setLoginPrompt] = useState<NavItem | null>(null)

  return (
    <nav className="grid grid-cols-5 border-t border-[var(--color-border)] bg-[var(--color-surface)] px-1 pt-2 pb-3.5">
      {NAV_ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          onClick={(e) => {
            if (item.loginRequired && !isLoggedIn()) {
              e.preventDefault()
              setLoginPrompt(item)
            }
          }}
          className={({ isActive }) =>
            `flex flex-col items-center gap-0.5 ${
              isActive ? 'text-[var(--color-accent)]' : 'text-[var(--color-label-alt)]'
            }`
          }
        >
          {({ isActive }) => (
            <>
              <span
                className="ms text-2xl"
                style={{ fontVariationSettings: `'FILL' ${isActive ? 1 : 0}` }}
              >
                {item.icon}
              </span>
              <span
                className={`text-[11px] whitespace-nowrap ${isActive ? 'font-bold' : 'font-medium'}`}
              >
                {item.label}
              </span>
            </>
          )}
        </NavLink>
      ))}
      {loginPrompt?.loginRequired && (
        <LoginRequiredDialog
          description={loginPrompt.loginRequired}
          onLogin={() => navigate(loginPath(loginPrompt.to))}
          onCancel={() => setLoginPrompt(null)}
        />
      )}
    </nav>
  )
}
