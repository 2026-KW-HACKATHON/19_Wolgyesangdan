import type { ActiveCampaign } from '../types/campaign'

/** YYYY-MM-DD를 로컬 자정 기준 Date로 (new Date('YYYY-MM-DD')는 UTC라 하루 어긋날 수 있다) */
function toLocalDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function startOfToday(now: Date) {
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

/** 오늘부터 그날까지 남은 날 수 (오늘이면 0, 지났으면 음수) */
function daysUntil(isoDate: string, now: Date) {
  const diff = toLocalDate(isoDate).getTime() - startOfToday(now).getTime()
  return Math.round(diff / (24 * 60 * 60 * 1000))
}

function monthDay(isoDate: string) {
  const date = toLocalDate(isoDate)
  return `${date.getMonth() + 1}.${date.getDate()}`
}

/** 배너에 보여주는 기간 — 신청 기간 "9.20 ~ 10.4" */
export function formatCampaignPeriod(campaign: ActiveCampaign) {
  return `${monthDay(campaign.applicationStartDate)} ~ ${monthDay(campaign.applicationEndDate)}`
}

/**
 * 배너의 D-day 문구. 신청 기간을 기준으로
 * 시작 전이면 "신청 시작까지 D-3", 신청 중이면 "신청 마감까지 D-12"(당일은 "오늘 신청 마감"),
 * 신청이 끝난 뒤(수령 기간)면 "신청 마감 · 10.6까지 수령"
 */
export function formatCampaignDday(campaign: ActiveCampaign, now: Date = new Date()) {
  const untilStart = daysUntil(campaign.applicationStartDate, now)
  if (untilStart > 0) return `신청 시작까지 D-${untilStart}`
  const untilEnd = daysUntil(campaign.applicationEndDate, now)
  if (untilEnd > 0) return `신청 마감까지 D-${untilEnd}`
  if (untilEnd === 0) return '오늘 신청 마감'
  return `신청 마감 · ${monthDay(campaign.pickupEndDate)}까지 수령`
}
