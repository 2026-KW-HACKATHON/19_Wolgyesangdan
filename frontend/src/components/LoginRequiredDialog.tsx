import Dialog from './Dialog'
import PrimaryButton from './PrimaryButton'

interface LoginRequiredDialogProps {
  /** 무엇을 하려면 로그인이 필요한지 */
  description: string
  onLogin: () => void
  onCancel: () => void
}

/** 로그인이 필요한 기능을 누르면 바로 로그인 화면으로 넘기지 않고 먼저 띄우는 팝업 (#173). 공용 틀은 Dialog (#192) */
export default function LoginRequiredDialog({ description, onLogin, onCancel }: LoginRequiredDialogProps) {
  return (
    <Dialog icon="lock" title="로그인이 필요해요" description={description} onClose={onCancel}>
      <PrimaryButton className="mt-5" label="로그인하러 가기" onClick={onLogin} />
      <PrimaryButton className="mt-1" variant="text" label="나중에 할게요" onClick={onCancel} />
    </Dialog>
  )
}
