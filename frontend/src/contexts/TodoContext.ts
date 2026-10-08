import { createContext, useContext } from 'react'
import type { MyTodo } from '../api/reservations'

export interface TodoContextValue {
  /** 내가 지금 해야 할 일. 비로그인이거나 아직 못 받았으면 빈 목록 */
  todo: MyTodo
  /** 재확인·전달 완료처럼 할 일이 바뀌는 동작 뒤에 다시 받는다 */
  refreshTodo: () => void
}

export const TodoContext = createContext<TodoContextValue | null>(null)

export function useTodo() {
  const value = useContext(TodoContext)
  if (!value) throw new Error('useTodo는 TodoProvider 안에서만 쓸 수 있습니다.')
  return value
}
