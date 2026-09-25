import { Outlet } from 'react-router-dom'
import BottomNav from '../components/BottomNav'
import PhoneFrame from './PhoneFrame'

export default function AppLayout() {
  return (
    <PhoneFrame>
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
      <BottomNav />
    </PhoneFrame>
  )
}
