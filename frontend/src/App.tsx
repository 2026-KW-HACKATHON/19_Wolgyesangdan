import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import DetailLayout from './layouts/DetailLayout'
import Home from './pages/Home'
import ItemList from './pages/ItemList'
import ContactSettingsPage from './pages/ContactSettingsPage'
import ContactProvider from './contexts/ContactProvider'
import CarbonReportPage from './pages/CarbonReportPage'
import MyPage from './pages/MyPage'
import MyVerificationPage from './pages/MyVerificationPage'
import ItemDetail from './pages/ItemDetail'
import ItemRegisterPage from './pages/ItemRegisterPage'
import KakaoCallbackPage from './pages/KakaoCallbackPage'
import LocationVerificationPage from './pages/LocationVerificationPage'
import LoginPage from './pages/LoginPage'
import PriorityChoicePage from './pages/PriorityChoicePage'
import PriorityDocumentPage from './pages/PriorityDocumentPage'
import VerificationDonePage from './pages/VerificationDonePage'
import { PRIORITY_TYPES } from './data/priorityVerification'
import { isLoggedIn } from './lib/authStorage'
import { loginPath, takeLoginNext } from './lib/loginRedirect'
import type { PrioritySubmitMeta, PriorityType } from './types/verification'

// 로그인이 필요한 화면 — 비로그인이면 로그인 화면으로 보내고, 로그인 후 이 화면으로 돌아오게 한다 (#173, #172).
// replace라서 뒤로가기로 막힌 화면에 다시 오지 않는다
function RequireLogin({ children }: { children: ReactNode }) {
  const location = useLocation()
  if (!isLoggedIn()) return <Navigate to={loginPath(location.pathname + location.search)} replace />
  return children
}

// 접수 완료 화면은 제출 결과(meta)를 router state로 넘겨받는다.
// state 없이 직접 진입하면 우선배정 선택으로 돌려보낸다.
function VerificationDoneRoute() {
  const navigate = useNavigate()
  const meta = useLocation().state as PrioritySubmitMeta | null

  if (!meta) return <Navigate to="/verify/priority" replace />

  return (
    <VerificationDonePage
      meta={meta}
      onGoHome={() => navigate('/', { replace: true })}
      onViewStatus={() => navigate('/mypage/verification', { replace: true })}
    />
  )
}

// /verify/priority/freshman | /verify/priority/basic
function PriorityDocumentRoute() {
  const navigate = useNavigate()
  const { type } = useParams()

  if (!PRIORITY_TYPES.includes(type as PriorityType)) return <Navigate to="/verify/priority" replace />

  return (
    <PriorityDocumentPage
      type={type as PriorityType}
      onBack={() => navigate('/verify/priority')}
      // 접수 완료에서 뒤로가기로 폼에 돌아오지 못하도록 history를 교체(replace)한다.
      onSubmitSuccess={(meta) => navigate('/verify/done', { replace: true, state: meta })}
    />
  )
}

function App() {
  const navigate = useNavigate()

  return (
    <ContactProvider>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/browse" element={<ItemList />} />
          <Route
            path="/register"
            element={
              <RequireLogin>
                <ItemRegisterPage />
              </RequireLogin>
            }
          />
          <Route path="/carbon-report" element={<CarbonReportPage />} />
          <Route path="/mypage" element={<MyPage />} />
        </Route>
        <Route element={<DetailLayout />}>
          <Route path="/items/:id" element={<ItemDetail />} />
          <Route path="/login" element={<LoginPage onBrowse={() => navigate('/')} />} />
          <Route path="/oauth/kakao/callback" element={<KakaoCallbackPage />} />
          {/* 로그인·인증 v2: GPS 동네 인증 → (선택) 우선배정 인증 */}
          <Route
            path="/verify/location"
            element={
              <LocationVerificationPage
                onBack={() => navigate(-1)}
                // 신규 회원이 로그인 전에 보던 화면이 있으면 그리로 (#172)
                onSkip={() => navigate(takeLoginNext() ?? '/', { replace: true })}
                onVerified={() => navigate('/verify/priority', { replace: true })}
              />
            }
          />
          <Route
            path="/verify/priority"
            element={
              <PriorityChoicePage
                onBack={() => navigate(-1)}
                onSkip={() => navigate(takeLoginNext() ?? '/', { replace: true })}
                onNext={(type) => navigate(`/verify/priority/${type}`)}
              />
            }
          />
          <Route path="/verify/priority/:type" element={<PriorityDocumentRoute />} />
          <Route path="/verify/done" element={<VerificationDoneRoute />} />
          {/* v1 주소(주민·학생 서류 인증)는 GPS 동네 인증으로 통합됐다 */}
          <Route path="/verification/*" element={<Navigate to="/verify/location" replace />} />
          <Route path="/settings/contact" element={<ContactSettingsPage />} />
          <Route path="/mypage/verification" element={<MyVerificationPage />} />
        </Route>
      </Routes>
    </ContactProvider>
  )
}

export default App
