import { Navigate, Route, Routes } from 'react-router-dom'
import AdminGate from './components/AdminGate'
import CampaignsPage from './pages/CampaignsPage'
import DashboardPage from './pages/DashboardPage'
import HubTradesPage from './pages/HubTradesPage'
import InquiriesPage from './pages/InquiriesPage'
import ItemsPage from './pages/ItemsPage'
import LoginPage from './pages/LoginPage'
import VerificationsPage from './pages/VerificationsPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AdminGate />}>
        <Route index element={<DashboardPage />} />
        <Route path="verifications" element={<VerificationsPage />} />
        <Route path="campaigns" element={<CampaignsPage />} />
        <Route path="inquiries" element={<InquiriesPage />} />
        <Route path="items" element={<ItemsPage />} />
        <Route path="hub-trades" element={<HubTradesPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
