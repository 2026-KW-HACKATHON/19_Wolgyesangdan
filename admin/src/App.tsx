import { Navigate, Route, Routes } from 'react-router-dom'
import AdminGate from './components/AdminGate'
import LoginPage from './pages/LoginPage'
import PreparingPage from './pages/PreparingPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AdminGate />}>
        <Route index element={<PreparingPage issue="#217" />} />
        <Route path="verifications" element={<PreparingPage issue="#208" />} />
        <Route path="campaigns" element={<PreparingPage issue="#210" />} />
        <Route path="inquiries" element={<PreparingPage issue="#213" />} />
        <Route path="items" element={<PreparingPage issue="#215" />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
