import { Route, Routes } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import Home from './pages/Home'
import ItemList from './pages/ItemList'
import Placeholder from './pages/Placeholder'

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/browse" element={<ItemList />} />
        <Route path="/register" element={<Placeholder title="등록" />} />
        <Route path="/carbon-report" element={<Placeholder title="탄소절감 리포트" />} />
        <Route path="/mypage" element={<Placeholder title="마이페이지" />} />
        <Route path="/items/:id" element={<Placeholder title="물품 상세" />} />
      </Route>
    </Routes>
  )
}

export default App
