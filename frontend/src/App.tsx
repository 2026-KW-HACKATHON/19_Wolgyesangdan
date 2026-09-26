import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import DetailLayout from './layouts/DetailLayout'
import Home from './pages/Home'
import ItemList from './pages/ItemList'
import ItemDetail from './pages/ItemDetail'
import LoginPage from './pages/LoginPage'
import Placeholder from './pages/Placeholder'
import ResidentVerificationFormPage from './pages/ResidentVerificationFormPage'
import StudentVerificationFormPage from './pages/StudentVerificationFormPage'
import VerificationCompletePage from './pages/VerificationCompletePage'
import VerificationMethodPage from './pages/VerificationMethodPage'
import type { VerificationSubmitMeta } from './types/verification'

// 접수 완료 화면은 제출 결과(meta)를 router state로 넘겨받는다.
// state 없이 직접 진입하면 인증 방법 선택으로 돌려보낸다.
function VerificationCompleteRoute() {
  const navigate = useNavigate()
  const meta = useLocation().state as VerificationSubmitMeta | null

  if (!meta) return <Navigate to="/verification" replace />

  return (
    <VerificationCompletePage
      meta={meta}
      onGoHome={() => navigate('/')}
      onViewStatus={() => navigate('/mypage')}
    />
  )
}

function App() {
  const navigate = useNavigate()

  // 접수 완료에서 뒤로가기로 폼에 돌아오지 못하도록 history를 교체(replace)한다.
  const handleSubmitSuccess = (meta: VerificationSubmitMeta) =>
    navigate('/verification/complete', { replace: true, state: meta })

  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/browse" element={<ItemList />} />
        <Route path="/register" element={<Placeholder title="등록" />} />
        <Route path="/carbon-report" element={<Placeholder title="탄소절감 리포트" />} />
        <Route path="/mypage" element={<Placeholder title="마이페이지" />} />
      </Route>
      <Route element={<DetailLayout />}>
        <Route path="/items/:id" element={<ItemDetail />} />
        <Route
          path="/login"
          element={<LoginPage onLogin={() => navigate('/verification')} onBrowse={() => navigate('/')} />}
        />
        <Route
          path="/verification"
          element={
            <VerificationMethodPage
              onBack={() => navigate('/login')}
              onSkip={() => navigate('/')}
              onNext={(type) => navigate(`/verification/${type}`)}
            />
          }
        />
        <Route
          path="/verification/resident"
          element={
            <ResidentVerificationFormPage
              onBack={() => navigate('/verification')}
              onSubmitSuccess={handleSubmitSuccess}
            />
          }
        />
        <Route
          path="/verification/student"
          element={
            <StudentVerificationFormPage
              onBack={() => navigate('/verification')}
              onSubmitSuccess={handleSubmitSuccess}
            />
          }
        />
        <Route path="/verification/complete" element={<VerificationCompleteRoute />} />
      </Route>
    </Routes>
  )
}

export default App
