package com.Wolgyesangdan.backend.domain.carbonreport.dto;

import java.time.LocalDate;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;

/**
 * 탄소절감 리포트. 거래 완료(Reservation.status=COMPLETED)된 건만 집계한다.
 * 나무 환산치 같은 표시 문구는 프론트가 carbonReductionKg로 직접 계산한다.
 *
 * @param scope 요청받은 범위 그대로
 * @param reusedCount 재사용된 물품 수
 * @param carbonReductionKg 위 물품들의 예상 탄소 절감량 합계
 * @param since (화면용 추가) 범위 안에서 첫 거래가 완료된 달 "YYYY-MM". 완료된 거래가 없으면 null
 * @param campaign (화면용 추가) scope=CAMPAIGN일 때 대상 캠페인. ALL이거나 캠페인이 없으면 null
 * @param monthlyTrend 최근 6개월, 오래된 달 → 최신 달. 거래가 없는 달은 0
 * @param dailyTrades (화면용 추가) scope=CAMPAIGN일 때 캠페인 시작일 ~ 오늘(종료일 넘으면 종료일)의 날짜별 거래 수. 그 외에는 빈 배열
 * @param categoryBreakdown 카테고리 대분류 5개 고정 순서(가구, 가전, 주방, 생활, 기타)
 * @param me (화면용 추가) 로그인 시 같은 범위의 내 기여분(전달·수령 합계). 비로그인이면 null
 */
public record CarbonReportResponse(
		ReportScope scope,
		long reusedCount,
		long carbonReductionKg,
		String since,
		ReportCampaign campaign,
		List<MonthlyCarbon> monthlyTrend,
		List<DailyTrade> dailyTrades,
		List<CategoryCarbon> categoryBreakdown,
		MyContribution me) {

	/** @param month "YYYY-MM" */
	public record MonthlyCarbon(String month, long carbonReductionKg) {
	}

	public record DailyTrade(LocalDate date, long count) {
	}

	/** @param ratio 전체 대비 비율 0~1 (소수 둘째 자리 반올림). 전체가 0이면 0 */
	public record CategoryCarbon(CategoryGroup categoryGroup, long carbonReductionKg, double ratio) {
	}

	/** @param startDate 등록·신청·수령 기간 중 가장 이른 날, endDate는 가장 늦은 날 */
	public record ReportCampaign(Long id, String name, CampaignStatus status, LocalDate startDate, LocalDate endDate) {

		public static ReportCampaign of(Campaign campaign, LocalDate today) {
			return new ReportCampaign(campaign.getId(), campaign.getName(), campaign.statusOn(today),
					campaign.periodStart(), campaign.periodEnd());
		}
	}

	public record MyContribution(long carbonReductionKg) {
	}

}
