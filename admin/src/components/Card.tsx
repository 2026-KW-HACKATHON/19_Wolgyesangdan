import type { ReactNode } from 'react'

/** 흰 카드 (배경 surface, 1px 테두리, r16). 패딩은 쓰는 곳에서 정한다 — 표는 패딩 없이 꽉 채운다 */
export default function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <section className={`overflow-hidden rounded-2xl border border-border bg-surface ${className}`}>{children}</section>
  )
}
