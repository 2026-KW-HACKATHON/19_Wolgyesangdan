export function formatNumber(n: number) {
  return n.toLocaleString('ko-KR')
}

/** 'YYYY-MM-DD' → 'M.D' */
export function formatMonthDay(date: string) {
  const [, m, d] = date.split('-')
  return `${Number(m)}.${Number(d)}`
}

/** 'YYYY-MM' → 'YYYY.MM' */
export function formatYearMonth(yearMonth: string) {
  return yearMonth.replace('-', '.')
}
