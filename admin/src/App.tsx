import { Navigate, Route, Routes } from 'react-router-dom'
import AdminGate from './components/AdminGate'
import CampaignsPage from './pages/CampaignsPage'
import ItemsPage from './pages/ItemsPage'
import LoginPage from './pages/LoginPage'
import PreparingPage from './pages/PreparingPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AdminGate />}>
        <Route index element={<PreparingPage issue="#217" />} />
        <Route path="verifications" element={<PreparingPage issue="#208" />} />
        <Route path="campaigns" element={<CampaignsPage />} />
        <Route path="inquiries" element={<PreparingPage issue="#213" />} />
        <Route path="items" element={<ItemsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
