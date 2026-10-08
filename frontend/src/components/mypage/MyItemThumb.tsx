import { useState } from 'react'
import MaterialIcon from '../icons/MaterialIcon'

interface MyItemThumbProps {
  imageUrl: string | null
  alt: string
  /** 크기·모서리 클래스 */
  className: string
  iconSize: number
}

/** 마이페이지 물품 카드 사진. 사진이 없거나 불러오지 못하면 상자 아이콘 (#244) */
export default function MyItemThumb({ imageUrl, alt, className, iconSize }: MyItemThumbProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const showImage = imageUrl !== null && imageUrl !== failedUrl

  return (
    <span className={`flex flex-none items-center justify-center overflow-hidden bg-primary-tint text-accent ${className}`}>
      {showImage ? (
        <img
          src={imageUrl}
          alt={alt}
          loading="lazy"
          onError={() => setFailedUrl(imageUrl)}
          className="h-full w-full object-cover"
        />
      ) : (
        <MaterialIcon name="inventory_2" size={iconSize} />
      )}
    </span>
  )
}
