import { Outlet } from 'react-router-dom'
import BottomNav from '../components/BottomNav'

export default function AppLayout() {
  return (
    <div className="flex min-h-dvh justify-center bg-[var(--color-bg)] sm:items-center sm:py-8">
      <div className="flex h-dvh w-full max-w-md flex-col overflow-hidden border-[var(--color-border)] bg-[var(--color-screen)] sm:h-[calc(100dvh-4rem)] sm:rounded-[28px] sm:border sm:shadow-[0_18px_40px_rgba(46,41,25,0.12)]">
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
        <BottomNav />
      </div>
    </div>
  )
}
