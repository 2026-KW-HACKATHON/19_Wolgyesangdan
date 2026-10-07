import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import LoginRequiredDialog from '../components/LoginRequiredDialog'
import { isLoggedIn } from '../lib/authStorage'
import { loginPath } from '../lib/loginRedirect'

/**
 * 로그인이 필요한 화면으로 가는 버튼용. 로그인 상태면 바로 이동하고,
 * 비로그인이면 "로그인이 필요해요" 팝업을 띄운다 — 로그인하면 가려던 화면으로 돌아온다 (#179, #172).
 * 돌려받은 dialog를 화면 안에 그려야 팝업이 보인다.
 */
export function useLoginGate() {
  const navigate = useNavigate()
  const [prompt, setPrompt] = useState<{ to: string; description: string } | null>(null)

  const go = (to: string, description: string) => {
    if (isLoggedIn()) navigate(to)
    else setPrompt({ to, description })
  }

  const dialog = prompt && (
    <LoginRequiredDialog
      description={prompt.description}
      onLogin={() => navigate(loginPath(prompt.to))}
      onCancel={() => setPrompt(null)}
    />
  )

  return { go, dialog }
}
