import Dialog, { type DialogTone } from './Dialog'
import PrimaryButton from './PrimaryButton'

interface ConfirmDialogProps {
  icon: string
  title: string
  description: string
  confirmLabel: string
  cancelLabel?: string
  /** 되돌릴 수 없는 동작이면 danger — 아이콘·확인 버튼을 경고색으로 */
  tone?: DialogTone
  onConfirm: () => void
  onCancel: () => void
}

/** "정말 할까요?"를 묻는 공용 확인 팝업 (#192) — window.confirm 대신 쓴다 */
export default function ConfirmDialog({
  icon,
  title,
  description,
  confirmLabel,
  cancelLabel = '취소',
  tone = 'primary',
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Dialog icon={icon} tone={tone} title={title} description={description} onClose={onCancel}>
      <PrimaryButton
        className="mt-5"
        variant={tone === 'danger' ? 'danger' : 'primary'}
        label={confirmLabel}
        onClick={onConfirm}
      />
      <PrimaryButton className="mt-1" variant="text" label={cancelLabel} onClick={onCancel} />
    </Dialog>
  )
}
