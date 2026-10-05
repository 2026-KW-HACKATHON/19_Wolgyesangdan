import { useState } from 'react'
import { CATEGORY_ICON } from '../lib/item'
import type { CategoryGroup } from '../types/item'

type ItemThumbProps = {
  imageUrl: string | null
  categoryGroup: CategoryGroup
  alt: string
  /** 크기·모서리 등 바깥 상자 클래스 */
  className: string
  /** 사진이 없을 때 보여주는 아이콘 크기 클래스 */
  iconClassName: string
}

/** 물품 사진. 사진이 없거나 불러오지 못하면 카테고리 아이콘을 보여준다. */
export default function ItemThumb({ imageUrl, categoryGroup, alt, className, iconClassName }: ItemThumbProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const showImage = imageUrl !== null && imageUrl !== failedUrl

  return (
    <div
      className={`flex items-center justify-center overflow-hidden bg-[#E7EBD8] text-[var(--color-primary)] ${className}`}
    >
      {showImage ? (
        <img
          src={imageUrl}
          alt={alt}
          loading="lazy"
          onError={() => setFailedUrl(imageUrl)}
          className="h-full w-full object-cover"
        />
      ) : (
        <span className={`ms ${iconClassName}`}>{CATEGORY_ICON[categoryGroup]}</span>
      )}
    </div>
  )
}
