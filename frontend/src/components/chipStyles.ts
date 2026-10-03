/** 단일/다중 선택 칩 공통 스타일. */
export function chipClassName(selected: boolean) {
  return `cursor-pointer rounded-full border px-[15px] py-2 text-[14px] ${
    selected
      ? 'border-primary bg-primary font-bold text-screen'
      : 'border-border bg-surface font-semibold text-ink-2'
  }`
}
