import { createContext, useContext } from 'react'
import type { AdminSummary } from '../types/summary'

/**
 * AdminLayout이 한 번 불러온 요약을 사이드바 배지 · 상단 바 캠페인 칩 · 대시보드가 같이 쓴다.
 * summary는 불러오는 중이거나 실패하면 null — failed로 둘을 구분한다.
 */
export interface SummaryContextValue {
  summary: AdminSummary | null
  failed: boolean
}

export const SummaryContext = createContext<SummaryContextValue>({ summary: null, failed: false })

export function useAdminSummary() {
  return useContext(SummaryContext)
}
