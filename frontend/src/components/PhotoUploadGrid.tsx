import type { ChangeEvent } from 'react'
import type { UploadedFile } from '../types/verification'
import MaterialIcon from './icons/MaterialIcon'

interface PhotoUploadGridProps {
  files: UploadedFile[]
  max?: number
  /** 썸네일 플레이스홀더 아이콘/색 (주민: description, 학생: badge) */
  thumbIcon: string
  /** 썸네일 배경·글리프 색 Tailwind 클래스 (예: "bg-sunken text-label-alt") */
  thumbClassName: string
  onAdd: (fileList: FileList) => void
  onRemove: (id: string) => void
  /** 카메라/파일 선택 보조 액션 행 노출 여부 */
  showQuickActions?: boolean
  helperText?: string
}

/** 서류 사진 업로드 — 썸네일 목록 + 추가 슬롯 (최대 max장). */
export default function PhotoUploadGrid({
  files,
  max = 3,
  thumbIcon,
  thumbClassName,
  onAdd,
  onRemove,
  showQuickActions,
  helperText,
}: PhotoUploadGridProps) {
  const canAddMore = files.length < max

  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      onAdd(e.target.files)
    }
    e.target.value = ''
  }

  return (
    <div className="px-5 pt-5">
      <div className="mb-[9px] flex items-center gap-1.5">
        <span className="text-[14px] font-bold text-body">
          서류 사진<span className="text-terracotta"> *</span>
        </span>
        <span className="ml-auto text-[12px] font-semibold text-label-alt">
          {files.length} / {max}
        </span>
      </div>

      <div className="flex flex-wrap gap-2.5">
        {files.map((file) => (
          <div
            key={file.id}
            className={`relative flex h-32 w-26 flex-none items-center justify-center overflow-hidden rounded-2xl border border-border ${thumbClassName}`}
          >
            <MaterialIcon name={thumbIcon} size={34} />
            <button
              type="button"
              onClick={() => onRemove(file.id)}
              aria-label={`${file.name} 삭제`}
              className="absolute top-1.5 right-1.5 flex size-[22px] cursor-pointer items-center justify-center rounded-full bg-label/72"
            >
              <MaterialIcon name="close" size={14} className="text-screen" />
            </button>
            <span className="absolute inset-x-0 bottom-0 truncate bg-label/72 py-1 text-center text-[11px] font-semibold text-screen">
              {file.name}
            </span>
          </div>
        ))}

        {canAddMore && (
          <label className="relative flex h-32 w-26 flex-none cursor-pointer flex-col items-center justify-center gap-1.5 rounded-2xl border border-dashed border-border-deep bg-surface text-accent">
            <MaterialIcon name="add_a_photo" size={26} />
            <span className="text-[12px] font-bold">사진 첨부</span>
            <input
              type="file"
              accept="image/*,application/pdf"
              multiple
              onChange={handleInputChange}
              className="sr-only"
            />
          </label>
        )}

        {showQuickActions && canAddMore && (
          <div className="flex min-w-[140px] flex-1 flex-col justify-center gap-3">
            <label className="relative flex cursor-pointer items-center gap-2 rounded-xl border border-border bg-surface px-[13px] py-[11px] text-[14px] font-bold text-label">
              <MaterialIcon name="photo_camera" size={18} className="text-accent" />
              카메라
              <input
                type="file"
                accept="image/*"
                capture="environment"
                onChange={handleInputChange}
                className="sr-only"
              />
            </label>
            <label className="relative flex cursor-pointer items-center gap-2 rounded-xl border border-border bg-surface px-[13px] py-[11px] text-[14px] font-bold text-label">
              <MaterialIcon name="folder_open" size={18} className="text-accent" />
              파일 선택
              <input
                type="file"
                accept="image/*,application/pdf"
                multiple
                onChange={handleInputChange}
                className="sr-only"
              />
            </label>
          </div>
        )}
      </div>

      {helperText && (
        <div className="mt-[9px] text-[12px] font-medium leading-[1.55] text-label-alt">
          {helperText}
        </div>
      )}
    </div>
  )
}
