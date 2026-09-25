import { Outlet } from 'react-router-dom'
import BottomNav from '../components/BottomNav'

export default function AppLayout() {
  return (
    <div className="mx-auto flex h-dvh w-full max-w-md flex-col overflow-hidden bg-[var(--color-bg)]">
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  )
}
