import { useState, type ChangeEvent } from 'react'
import type { UploadedFile } from '../types/verification'
import MaterialIcon from './icons/MaterialIcon'

interface PhotoUploadGridProps {
  /** 칸 제목 (예: "물품 사진", "서류 사진") */
  label: string
  files: UploadedFile[]
  max?: number
  /** 미리보기가 없는 파일(PDF 등)이나 미리보기를 못 그릴 때 보여주는 아이콘 */
  thumbIcon: string
  /** 썸네일 배경·글리프 색 Tailwind 클래스 (예: "bg-sunken text-label-alt") */
  thumbClassName: string
  onAdd: (fileList: FileList) => void
  onRemove: (id: string) => void
  /** 카메라/파일 선택 보조 액션 행 노출 여부 */
  showQuickActions?: boolean
  helperText?: string
  /** 사진 첨부·파일 선택의 accept (카메라는 항상 image/*) */
  accept?: string
}

/** 사진 업로드 (물품 등록·우선배정 서류) — 썸네일 목록 + 추가 슬롯 (최대 max장). */
export default function PhotoUploadGrid({
  label,
  files,
  max = 3,
  thumbIcon,
  thumbClassName,
  onAdd,
  onRemove,
  showQuickActions,
  helperText,
  accept = 'image/*,application/pdf',
}: PhotoUploadGridProps) {
  const canAddMore = files.length < max
  // 미리보기를 그리지 못한 파일 (Chrome·Android가 못 여는 HEIC 등) — 아이콘으로 되돌린다
  const [brokenPreviewIds, setBrokenPreviewIds] = useState<string[]>([])

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
          {label}
          <span className="text-terracotta"> *</span>
        </span>
        <span className="ml-auto text-[12px] font-semibold text-label-alt">
          {files.length} / {max}
        </span>
      </div>

      <div className="flex flex-wrap gap-2.5">
        {files.map((file) => {
          // 방금 고른 사진은 로컬 미리보기(url), 그게 없으면 서버에 올라간 주소(imageUrl)로 보여준다
          const previewUrl = brokenPreviewIds.includes(file.id) ? undefined : (file.url ?? file.imageUrl)
          return (
            <div
              key={file.id}
              className={`relative flex h-32 w-26 flex-none items-center justify-center overflow-hidden rounded-2xl border border-border ${thumbClassName}`}
            >
              {previewUrl ? (
                <img
                  src={previewUrl}
                  alt=""
                  onError={() => setBrokenPreviewIds((prev) => [...prev, file.id])}
                  className="absolute inset-0 h-full w-full object-cover"
                />
              ) : (
                <MaterialIcon name={thumbIcon} size={34} />
              )}
              {file.status === 'uploading' && (
                <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-surface/80 text-[12px] font-bold text-label-alt">
                  <span className="size-[18px] animate-spin rounded-full border-2 border-accent/30 border-t-accent" />
                  올리는 중
                </div>
              )}
              {file.status === 'error' && (
                <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-surface/85 text-[12px] font-bold text-terracotta">
                  <MaterialIcon name="error" size={22} />
                  올리지 못했어요
                </div>
              )}
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
          )
        })}

        {canAddMore && (
          <label className="relative flex h-32 w-26 flex-none cursor-pointer flex-col items-center justify-center gap-1.5 rounded-2xl border border-dashed border-border-deep bg-surface text-accent">
            <MaterialIcon name="add_a_photo" size={26} />
            <span className="text-[12px] font-bold">사진 첨부</span>
            <input
              type="file"
              accept={accept}
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
                accept={accept}
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
