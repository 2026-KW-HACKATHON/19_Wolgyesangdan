interface MarketLogoProps {
  width?: number
  height?: number
  className?: string
}

/**
 * 월계상단 로고 — 시장 차양(awning) 모티프.
 * 디자인 핸드오프의 인라인 SVG 스케치를 그대로 옮긴 임시 로고입니다.
 * README: "확정 전 디자이너 정리 또는 원본 로고 파일 교체를 권합니다."
 */
export default function MarketLogo({ width = 180, height = 104, className }: MarketLogoProps) {
  return (
    <svg
      width={width}
      height={height}
      viewBox="0 0 180 104"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="월계상단 로고"
      className={className}
    >
      <rect x="86" y="2" width="8" height="12" rx="4" fill="#4F6B2A" />
      <path d="M14 36 L90 12 L166 36 L166 48 L14 48 Z" fill="#5E7F35" />
      <path d="M14 48 h25.3 v26 c-12.65 0 -12.65 14 -25.3 14 Z" fill="#B9603A" />
      <path
        d="M39.3 48 h25.4 v26 c-12.7 0 -12.7 14 -25.4 14 c0 -14 0 -14 0 -14 Z"
        fill="#E4A574"
      />
      <path
        d="M64.7 48 h25.3 v26 c-12.65 0 -12.65 14 -25.3 14 c0 -14 0 -14 0 -14 Z"
        fill="#B9603A"
      />
      <path
        d="M90 48 h25.3 v26 c-12.65 0 -12.65 14 -25.3 14 c0 -14 0 -14 0 -14 Z"
        fill="#E4A574"
      />
      <path
        d="M115.3 48 h25.4 v26 c-12.7 0 -12.7 14 -25.4 14 c0 -14 0 -14 0 -14 Z"
        fill="#B9603A"
      />
      <path
        d="M140.7 48 h25.3 v26 c-12.65 0 -12.65 14 -25.3 14 c0 -14 0 -14 0 -14 Z"
        fill="#E4A574"
      />
      <path d="M14 48 h152" stroke="#3F5A21" strokeWidth="4" />
    </svg>
  )
}
