import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import DetailLayout from './layouts/DetailLayout'
import Home from './pages/Home'
import ItemList from './pages/ItemList'
import ContactSettingsPage from './pages/ContactSettingsPage'
import ContactProvider from './contexts/ContactProvider'
import TodoProvider from './contexts/TodoProvider'
import LoginRequiredDialog from './components/LoginRequiredDialog'
import SessionExpiredDialog from './components/SessionExpiredDialog'
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

// 로그인이 필요한 화면 — 주소를 직접 입력해 들어온 비로그인 사용자에게 "로그인이 필요해요" 팝업을 띄운다 (#173).
// 로그인하면 이 화면으로 돌아온다(#172). 바텀 탭에서 누른 경우는 BottomNav가 이동 전에 같은 팝업을 띄운다.
// replace라서 뒤로가기로 막힌 화면에 다시 오지 않는다
function RequireLogin({ description, children }: { description: string; children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  if (isLoggedIn()) return children
  return (
    <LoginRequiredDialog
      description={description}
      onLogin={() => navigate(loginPath(location.pathname + location.search), { replace: true })}
      onCancel={() => navigate('/', { replace: true })}
    />
  )
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
      <TodoProvider>
      <SessionExpiredDialog />
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/browse" element={<ItemList />} />
          <Route
            path="/register"
            element={
              <RequireLogin description="물품을 등록하려면 로그인해 주세요. 로그인하면 바로 등록 화면으로 이어져요.">
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
          <Route
            path="/settings/contact"
            element={
              <RequireLogin description="연락 수단을 설정하려면 로그인해 주세요. 로그인하면 바로 설정 화면으로 이어져요.">
                <ContactSettingsPage />
              </RequireLogin>
            }
          />
          <Route
            path="/mypage/verification"
            element={
              <RequireLogin description="인증하려면 로그인해 주세요. 로그인하면 바로 인증 화면으로 이어져요.">
                <MyVerificationPage />
              </RequireLogin>
            }
          />
        </Route>
      </Routes>
      </TodoProvider>
    </ContactProvider>
  )
}

export default App
