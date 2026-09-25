import { Outlet } from 'react-router-dom'
import PhoneFrame from './PhoneFrame'

// 목록/홈처럼 바텀 탭에 속한 화면이 아니라, 뒤로가기로 드릴다운하는 화면용 레이아웃.
// 바텀 탭바를 보여주지 않는다는 점만 AppLayout과 다르다.
export default function DetailLayout() {
  return (
    <PhoneFrame>
      <Outlet />
    </PhoneFrame>
  )
}
