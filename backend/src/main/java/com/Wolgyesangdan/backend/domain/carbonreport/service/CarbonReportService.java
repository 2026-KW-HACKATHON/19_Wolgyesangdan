package com.Wolgyesangdan.backend.domain.carbonreport.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.CategoryCarbon;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.DailyTrade;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.MonthlyCarbon;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.MyContribution;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.ReportCampaign;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.MyImpactResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.ReportScope;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.reservation.dto.CategoryCarbonSum;
import com.Wolgyesangdan.backend.domain.reservation.dto.CompletedTrade;
import com.Wolgyesangdan.backend.domain.reservation.dto.CompletedTradeSummary;
import com.Wolgyesangdan.backend.domain.reservation.dto.TradeCounts;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탄소 절감 성과 집계. 자기 엔티티 없이 거래 완료된 예약·물품을 모아 계산한다.
 * ERD 결정대로 캐시 필드 없이 조회할 때마다 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarbonReportService {

	/** 월별 추이 개월 수 (이번 달 포함) */
	static final int TREND_MONTHS = 6;

	private final ReservationRepository reservationRepository;
	private final CampaignService campaignService;

	public MyImpactResponse getMyImpact(Long userId) {
		TradeCounts counts = reservationRepository.summarizeTradesByUserId(userId, null);
		return new MyImpactResponse(counts.givenCount(), counts.receivedCount(), counts.carbonReductionKg());
	}

	/**
	 * @param userId 로그인 사용자 id. 비로그인이면 null (내 기여분 me를 빼고 내려준다)
	 */
	public CarbonReportResponse getReport(ReportScope scope, Long userId) {
		return getReport(scope, userId, LocalDate.now());
	}

	CarbonReportResponse getReport(ReportScope scope, Long userId, LocalDate today) {
		if (scope == ReportScope.ALL) {
			return buildReport(scope, null, userId, today);
		}
		// 캠페인 범위인데 진행 중·예정 캠페인이 없으면 에러가 아니라 전부 0 (캠페인 조회 API와 같은 패턴)
		return campaignService.findCurrentCampaign(today)
				.map(campaign -> buildReport(scope, campaign, userId, today))
				.orElseGet(() -> emptyCampaignReport(userId));
	}

	private CarbonReportResponse buildReport(ReportScope scope, Campaign campaign, Long userId, LocalDate today) {
		Long campaignId = campaign == null ? null : campaign.getId();
		CompletedTradeSummary summary = reservationRepository.summarizeCompleted(campaignId);

		YearMonth thisMonth = YearMonth.from(today);
		YearMonth firstTrendMonth = thisMonth.minusMonths(TREND_MONTHS - 1);
		// 월별 추이와 날짜별 거래에 필요한 기간을 한 번에 가져온다
		LocalDate from = campaign == null
				? firstTrendMonth.atDay(1)
				: min(firstTrendMonth.atDay(1), campaign.periodStart());
		List<CompletedTrade> trades = reservationRepository.findCompletedSince(from.atStartOfDay(), campaignId);

		return new CarbonReportResponse(
				scope,
				summary.count(),
				summary.carbonReductionKg(),
				Optional.ofNullable(reservationRepository.findFirstCompletedAt(campaignId))
						.map(first -> YearMonth.from(first).toString())
						.orElse(null),
				campaign == null ? null : ReportCampaign.of(campaign, today),
				monthlyTrend(trades, firstTrendMonth, thisMonth),
				campaign == null ? List.of() : dailyTrades(trades, campaign, today),
				categoryBreakdown(reservationRepository.sumCompletedByCategoryGroup(campaignId),
						summary.carbonReductionKg()),
				myContribution(userId, campaignId));
	}

	private CarbonReportResponse emptyCampaignReport(Long userId) {
		return new CarbonReportResponse(ReportScope.CAMPAIGN, 0, 0, null, null, List.of(), List.of(),
				categoryBreakdown(List.of(), 0), userId == null ? null : new MyContribution(0));
	}

	/** first ~ last 달마다 한 칸, 거래가 없는 달은 0 */
	private static List<MonthlyCarbon> monthlyTrend(List<CompletedTrade> trades, YearMonth first, YearMonth last) {
		Map<YearMonth, Long> kgByMonth = trades.stream().collect(Collectors.groupingBy(
				trade -> YearMonth.from(trade.completedAt()),
				Collectors.summingLong(CompletedTrade::carbonReductionKg)));
		return Stream.iterate(first, month -> !month.isAfter(last), month -> month.plusMonths(1))
				.map(month -> new MonthlyCarbon(month.toString(), kgByMonth.getOrDefault(month, 0L)))
				.toList();
	}

	/** 캠페인 시작일 ~ 오늘(종료일이 지났으면 종료일) 하루마다 한 칸. 아직 시작 전이면 빈 배열 */
	private static List<DailyTrade> dailyTrades(List<CompletedTrade> trades, Campaign campaign, LocalDate today) {
		LocalDate last = min(today, campaign.periodEnd());
		if (last.isBefore(campaign.periodStart())) {
			return List.of();
		}
		Map<LocalDate, Long> countByDate = trades.stream().collect(Collectors.groupingBy(
				trade -> trade.completedAt().toLocalDate(), Collectors.counting()));
		return campaign.periodStart().datesUntil(last.plusDays(1))
				.map(date -> new DailyTrade(date, countByDate.getOrDefault(date, 0L)))
				.toList();
	}

	/** 대분류 5개를 고정 순서로, 거래가 없는 대분류도 0으로 채운다 */
	private static List<CategoryCarbon> categoryBreakdown(List<CategoryCarbonSum> sums, long totalKg) {
		Map<CategoryGroup, Long> kgByGroup = sums.stream()
				.collect(Collectors.toMap(CategoryCarbonSum::categoryGroup, CategoryCarbonSum::carbonReductionKg));
		return Arrays.stream(CategoryGroup.values())
				.map(group -> {
					long kg = kgByGroup.getOrDefault(group, 0L);
					return new CategoryCarbon(group, kg, ratio(kg, totalKg));
				})
				.toList();
	}

	private MyContribution myContribution(Long userId, Long campaignId) {
		if (userId == null) {
			return null;
		}
		return new MyContribution(reservationRepository.summarizeTradesByUserId(userId, campaignId).carbonReductionKg());
	}

	/** 소수 둘째 자리 반올림, 전체가 0이면 0 */
	private static double ratio(long part, long total) {
		return total == 0 ? 0 : Math.round(part * 100.0 / total) / 100.0;
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

}
