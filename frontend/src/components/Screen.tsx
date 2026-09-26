import type { ReactNode } from 'react'

interface ScreenProps {
  children: ReactNode
}

/**
 * 화면 루트 — layouts/PhoneFrame 안에서 남은 높이를 채우고 스스로 스크롤한다.
 * 폭·라운드·그림자 같은 프레임 처리는 PhoneFrame이 맡는다.
 * leading-[normal]: preflight의 line-height 1.5 대신 시안 기준(normal)을 유지.
 */
export default function Screen({ children }: ScreenProps) {
  return (
    <div className="flex min-h-0 flex-1 flex-col overflow-y-auto bg-screen leading-[normal]">
      {children}
    </div>
  )
}
