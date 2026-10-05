import { useId } from 'react'

// 유리 안쪽 모양 (24×24 기준). 가운데(y=12) 목을 기준으로 위·아래 방이 대칭이다.
const TOP_CHAMBER = 'M7 4 H17 V7 C17 9.5 13.5 11 13.5 12 H10.5 C10.5 11 7 9.5 7 7 Z'
const BOTTOM_CHAMBER = 'M10.5 12 H13.5 C13.5 13 17 14.5 17 17 V20 H7 V17 C7 14.5 10.5 13 10.5 12 Z'
// 두 방을 이은 유리 테두리 (목을 가로지르는 선이 없도록 한 줄로 그린다)
const GLASS =
  'M7 4 H17 V7 C17 9.5 13.5 11 13.5 12 C13.5 13 17 14.5 17 17 V20 H7 V17 C7 14.5 10.5 13 10.5 12 C10.5 11 7 9.5 7 7 Z'

interface HourglassIconProps {
  size?: number
  className?: string
}

/**
 * 모래가 떨어지는 모래시계. 위쪽 모래가 다 내려오면 180° 뒤집혀서 다시 떨어진다.
 * 위·아래가 대칭이라 뒤집힌 모습이 곧 처음 모습이어서 반복이 끊기지 않는다 (index.css의 hourglass-*).
 * 색은 currentColor를 따르고, "동작 줄이기" 설정이면 위쪽이 찬 채로 멈춰 있다.
 */
export default function HourglassIcon({ size = 24, className }: HourglassIconProps) {
  const id = useId()
  const topClip = `${id}-top`
  const bottomClip = `${id}-bottom`

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      aria-hidden="true"
      className={['animate-hourglass-flip motion-reduce:animate-none', className].filter(Boolean).join(' ')}
    >
      <defs>
        <clipPath id={topClip}>
          <path d={TOP_CHAMBER} />
        </clipPath>
        <clipPath id={bottomClip}>
          <path d={BOTTOM_CHAMBER} />
        </clipPath>
      </defs>

      {/* 위쪽 모래 — 아래로 밀려 내려가면서 방 밖으로 나간 부분은 잘린다 */}
      <g clipPath={`url(#${topClip})`}>
        <rect
          x="6"
          y="4"
          width="12"
          height="8"
          fill="currentColor"
          className="animate-hourglass-drain motion-reduce:animate-none"
        />
      </g>
      {/* 아래쪽 모래와 떨어지는 줄기 — 멈춘 상태에서는 비어 있다 */}
      <g clipPath={`url(#${bottomClip})`}>
        <rect
          x="6"
          y="12"
          width="12"
          height="8"
          fill="currentColor"
          className="animate-hourglass-fill motion-reduce:animate-none"
          style={{ transform: 'translateY(8px)' }}
        />
        <rect
          x="11.5"
          y="12"
          width="1"
          height="8"
          fill="currentColor"
          className="animate-hourglass-stream opacity-0 motion-reduce:animate-none"
        />
      </g>

      {/* 유리와 위·아래 받침 */}
      <path d={GLASS} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M6 3.4 H18 M6 20.6 H18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}
