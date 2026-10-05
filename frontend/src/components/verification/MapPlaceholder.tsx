import MaterialIcon from '../icons/MaterialIcon'

interface MapPlaceholderProps {
  /** 현재 위치 점 — 월계1동 안(inside) / 밖(outside) / 아직 모름(null, 점 숨김) */
  dot: 'inside' | 'outside' | null
  onRelocate: () => void
  relocateDisabled?: boolean
}

/**
 * 지도 자리표시 (시안 1b의 CSS 지도). 길·블록·월계1동 경계·현재 위치 점만 그린다.
 * TODO: 카카오맵 JS SDK + 월계1동 행정동 경계 GeoJSON으로 교체
 */
export default function MapPlaceholder({ dot, onRelocate, relocateDisabled }: MapPlaceholderProps) {
  return (
    <div className="relative h-[250px] overflow-hidden bg-[#E9E6D6]">
      <div aria-hidden="true">
        <div className="absolute top-[92px] -right-5 -left-5 h-3.5 -rotate-8 bg-surface" />
        <div className="absolute -top-5 -bottom-5 left-[150px] w-3 rotate-14 bg-surface" />
        <div className="absolute -top-5 -bottom-5 left-[286px] w-[9px] -rotate-6 bg-surface" />
        <div className="absolute top-[186px] -right-5 -left-5 h-[9px] rotate-4 bg-surface" />
        <div className="absolute top-[22px] left-[22px] h-[58px] w-24 rounded-[10px] bg-[#DDE3C7]" />
        <div className="absolute top-[120px] right-5 h-[54px] w-[70px] rounded-[10px] bg-[#DDE3C7]" />
        <div className="absolute top-7 left-[58px] h-[200px] w-60 rounded-[46%_54%_42%_58%] border-3 border-dashed border-primary bg-primary/12" />
        <span className="absolute top-10 left-[72px] rounded-[7px] bg-surface px-2 py-[3px] text-[12px] font-bold text-primary-dark">
          월계1동
        </span>
        {dot && (
          <span
            className={`absolute flex size-[46px] items-center justify-center rounded-full bg-primary/22 ${
              dot === 'inside' ? 'top-[104px] left-[178px]' : 'top-[184px] left-[6px]'
            }`}
          >
            <span className="box-border size-[18px] rounded-full border-3 border-surface bg-primary" />
          </span>
        )}
      </div>
      <button
        type="button"
        onClick={onRelocate}
        disabled={relocateDisabled}
        aria-label="현재 위치 다시 찾기"
        className="absolute right-3 bottom-3 flex size-10 cursor-pointer items-center justify-center rounded-xl bg-surface text-accent shadow-[0_2px_6px_rgba(46,41,25,0.14)] disabled:cursor-default disabled:opacity-60"
      >
        <MaterialIcon name="my_location" size={22} />
      </button>
    </div>
  )
}
