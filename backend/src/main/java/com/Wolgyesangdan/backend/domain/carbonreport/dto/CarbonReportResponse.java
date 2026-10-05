package com.Wolgyesangdan.backend.domain.carbonreport.dto;

import java.time.LocalDate;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;

/**
 * 탄소절감 리포트 (API 명세 · 탄소절감 리포트 조회, 2026-10-05 동네 나무 디자인 반영).
 * 거래 완료(Reservation.status=COMPLETED)된 건만 집계한다.
 * 나무 그루 수·레벨·열매 수 같은 표시 값은 프론트가 kg·개수로 직접 계산한다.
 *
 * @param scope 요청받은 범위 그대로
 * @param reusedCount 거래 완료된 물품 수
 * @param carbonReductionKg 위 물품들의 예상 탄소 절감량 합계
 * @param categoryBreakdown 카테고리 대분류 5개 고정 순서(가구, 가전, 주방, 생활, 기타)
 * @param myCarbonReductionKg 로그인 시 같은 범위에서 내가 등록해서(나눔) 거래 완료된 물품의 탄소 합계. 비회원이면 null
 * @param since scope=ALL일 때만 첫 거래 완료 월 "YYYY-MM". 거래가 없거나 CAMPAIGN이면 null
 * @param campaign scope=CAMPAIGN일 때만 대상 캠페인. 캠페인이 없거나 ALL이면 null
 * @param dailyTrend scope=CAMPAIGN일 때만 캠페인 시작일 ~ 오늘(종료일이 더 이르면 종료일) 날짜별 거래 완료 수. 그 외에는 빈 배열
 */
public record CarbonReportResponse(
		ReportScope scope,
		long reusedCount,
		long carbonReductionKg,
		List<CategoryCarbon> categoryBreakdown,
		Long myCarbonReductionKg,
		String since,
		ReportCampaign campaign,
		List<DailyTrade> dailyTrend) {

	/** @param ratio 전체 대비 비율 0~1 (소수 둘째 자리 반올림). 전체가 0이면 0 */
	public record CategoryCarbon(CategoryGroup categoryGroup, long carbonReductionKg, double ratio) {
	}

	/**
	 * @param status PLANNED 또는 ACTIVE (리포트 대상은 진행 중, 없으면 예정 캠페인)
	 * @param startDate 등록·신청·수령 기간 중 가장 이른 시작일, endDate는 가장 늦은 종료일 — 캠페인 진행 여부 판단과 같은 기준
	 */
	public record ReportCampaign(Long id, String name, CampaignStatus status, LocalDate startDate, LocalDate endDate) {

		public static ReportCampaign of(Campaign campaign, LocalDate today) {
			return new ReportCampaign(campaign.getId(), campaign.getName(), campaign.statusOn(today),
					campaign.periodStart(), campaign.periodEnd());
		}
	}

	public record DailyTrade(LocalDate date, long count) {
	}

}
