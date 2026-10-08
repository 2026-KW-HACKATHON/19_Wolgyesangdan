import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { getMyTodo, type MyTodo } from '../api/reservations'
import { getAccessToken } from '../lib/authStorage'
import { TodoContext } from './TodoContext'

const EMPTY_TODO: MyTodo = { reconfirms: [], deliveries: [] }

/**
 * 내가 지금 해야 할 일 (GET /users/me/todo, #188) — 홈 배너·아래 탭·마이페이지 탭이 함께 쓴다.
 * 배정·노쇼 승계는 서버 스케줄러가 바꾸므로 화면을 옮길 때와 앱으로 돌아올 때(포커스) 다시 받는다.
 */
export default function TodoProvider({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  const token = getAccessToken()
  // 어떤 로그인(토큰)으로 받은 값인지 함께 둬서, 로그아웃·다른 계정이면 이전 값을 쓰지 않는다
  const [loaded, setLoaded] = useState<{ token: string; todo: MyTodo } | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!token) return
    let ignore = false
    getMyTodo()
      .then((todo) => {
        if (!ignore) setLoaded({ token, todo })
      })
      .catch(() => {
        // 알림용이라 실패해도 화면은 그대로 둔다 (로그인이 풀린 경우는 apiFetch가 팝업으로 처리)
      })
    return () => {
      ignore = true
    }
  }, [token, pathname, version])

  useEffect(() => {
    const handleFocus = () => setVersion((v) => v + 1)
    window.addEventListener('focus', handleFocus)
    return () => window.removeEventListener('focus', handleFocus)
  }, [])

  const todo = token !== null && loaded?.token === token ? loaded.todo : EMPTY_TODO
  const refreshTodo = useCallback(() => setVersion((v) => v + 1), [])
  const value = useMemo(() => ({ todo, refreshTodo }), [todo, refreshTodo])

  return <TodoContext.Provider value={value}>{children}</TodoContext.Provider>
}
