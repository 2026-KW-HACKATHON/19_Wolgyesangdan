package com.Wolgyesangdan.backend.domain.carbonreport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.CategoryCarbon;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.DailyTrade;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.ReportCampaign;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.MyImpactResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.ReportScope;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.reservation.dto.CategoryCarbonSum;
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

	/** 전체 범위(campaignId = null)에서는 기간 조건이 쓰이지 않는다 — 쿼리 파라미터를 채우기만 하는 값 */
	private static final LocalDateTime NO_PERIOD = LocalDate.EPOCH.atStartOfDay();

	private final ReservationRepository reservationRepository;
	private final CampaignService campaignService;

	public MyImpactResponse getMyImpact(Long userId) {
		TradeCounts counts = reservationRepository.summarizeTradesByUserId(userId);
		return new MyImpactResponse(counts.givenCount(), counts.receivedCount(), counts.carbonReductionKg());
	}

	/**
	 * @param userId 로그인 사용자 id. 비회원(토큰이 없거나 잘못됨)이면 null — myCarbonReductionKg를 null로 내려준다
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
				.orElseGet(() -> buildEmptyCampaignReport(userId));
	}

	private CarbonReportResponse buildReport(ReportScope scope, Campaign campaign, Long userId, LocalDate today) {
		Long campaignId = campaign == null ? null : campaign.getId();
		// 캠페인 범위는 그 캠페인 물품(거점 거래) + 캠페인 기간 안에 완료된 직거래 (#248)
		LocalDateTime from = campaign == null ? NO_PERIOD : campaign.periodStartAt();
		LocalDateTime to = campaign == null ? NO_PERIOD : campaignService.directTradePeriodEnd(campaign);
		// 대분류별 합을 더해 전체 물품 수·탄소 합계를 구한다 (같은 행을 두 번 집계하지 않도록)
		List<CategoryCarbonSum> sums = reservationRepository.sumCompletedByCategoryGroup(campaignId, from, to);
		long reusedCount = sums.stream().mapToLong(CategoryCarbonSum::count).sum();
		long totalKg = sums.stream().mapToLong(CategoryCarbonSum::carbonReductionKg).sum();

		return new CarbonReportResponse(
				scope,
				reusedCount,
				totalKg,
				categoryBreakdown(sums, totalKg),
				myCarbonReductionKg(userId, campaignId, from, to),
				campaign == null ? since() : null,
				campaign == null ? null : ReportCampaign.of(campaign, today),
				campaign == null ? List.of() : dailyTrend(campaign, today));
	}

	private CarbonReportResponse buildEmptyCampaignReport(Long userId) {
		return new CarbonReportResponse(ReportScope.CAMPAIGN, 0, 0, categoryBreakdown(List.of(), 0),
				userId == null ? null : 0L, null, null, List.of());
	}

	/** 월계1동 전체에서 첫 거래가 완료된 달 "YYYY-MM". 거래가 없으면 null */
	private String since() {
		return Optional.ofNullable(reservationRepository.findFirstCompletedAt())
				.map(first -> YearMonth.from(first).toString())
				.orElse(null);
	}

	/** 캠페인 시작일 ~ 오늘(종료일이 더 이르면 종료일) 하루마다 한 칸, 거래 없는 날은 0. 아직 시작 전이면 빈 배열 */
	private List<DailyTrade> dailyTrend(Campaign campaign, LocalDate today) {
		LocalDate start = campaign.periodStart();
		LocalDate last = min(today, campaign.periodEnd());
		if (last.isBefore(start)) {
			return List.of();
		}
		Map<LocalDate, Long> countByDate = reservationRepository
				.findCompletedAtInCampaign(campaign.getId(), campaign.periodStartAt(),
						campaignService.directTradePeriodEnd(campaign))
				.stream()
				.collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
		return start.datesUntil(last.plusDays(1))
				.map(date -> new DailyTrade(date, countByDate.getOrDefault(date, 0L)))
				.toList();
	}

	/** 대분류 5개를 고정 순서로, 거래가 없는 대분류도 0으로 채운다 */
	private static List<CategoryCarbon> categoryBreakdown(List<CategoryCarbonSum> sums, long totalKg) {
		Map<CategoryGroup, CategoryCarbonSum> byGroup = sums.stream()
				.collect(Collectors.toMap(CategoryCarbonSum::categoryGroup, Function.identity()));
		return Arrays.stream(CategoryGroup.values())
				.map(group -> {
					long kg = Optional.ofNullable(byGroup.get(group)).map(CategoryCarbonSum::carbonReductionKg).orElse(0L);
					return new CategoryCarbon(group, kg, ratio(kg, totalKg));
				})
				.toList();
	}

	/** 내가 등록해서(나눔) 거래 완료된 물품의 탄소 합계 — 비회원이면 null */
	private Long myCarbonReductionKg(Long userId, Long campaignId, LocalDateTime from, LocalDateTime to) {
		return userId == null ? null : reservationRepository.sumGivenCarbonByUserId(userId, campaignId, from, to);
	}

	/** 소수 둘째 자리 반올림, 전체가 0이면 0 */
	private static double ratio(long part, long total) {
		return total == 0 ? 0 : Math.round(part * 100.0 / total) / 100.0;
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

}
