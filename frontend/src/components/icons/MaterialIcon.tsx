import type { CSSProperties } from 'react'

interface MaterialIconProps {
  /** Material Symbols Rounded glyph name, e.g. "chat_bubble" */
  name: string
  size?: number
  color?: string
  className?: string
  style?: CSSProperties
}

/**
 * Material Symbols Rounded 아이콘 글리프.
 * 폰트 로드는 index.html의 Google Fonts 링크에서 처리합니다.
 */
export default function MaterialIcon({
  name,
  size = 24,
  color,
  className,
  style,
}: MaterialIconProps) {
  return (
    <span
      className={['ms', className].filter(Boolean).join(' ')}
      style={{ fontSize: size, color, ...style }}
      aria-hidden="true"
    >
      {name}
    </span>
  )
}
