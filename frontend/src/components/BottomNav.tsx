import { NavLink } from 'react-router-dom'

type NavItem = {
  to: string
  label: string
  icon: string
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/browse', label: '둘러보기', icon: 'explore' },
  { to: '/register', label: '등록', icon: 'add_box' },
  { to: '/carbon-report', label: '탄소절감 리포트', icon: 'autorenew' },
  { to: '/mypage', label: '마이페이지', icon: 'person' },
]

export default function BottomNav() {
  return (
    <nav className="grid grid-cols-5 border-t border-[var(--color-border)] bg-[var(--color-surface)] px-1 pt-2 pb-3.5">
      {NAV_ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
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
    </nav>
  )
}
