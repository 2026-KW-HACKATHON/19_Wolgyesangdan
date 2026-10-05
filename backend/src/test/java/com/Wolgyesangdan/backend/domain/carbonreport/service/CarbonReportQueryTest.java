package com.Wolgyesangdan.backend.domain.carbonreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.CategoryCarbon;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.DailyTrade;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.ReportScope;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 탄소절감 리포트 집계 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, CarbonReportService.class, CampaignService.class})
class CarbonReportQueryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

	@Autowired
	private CarbonReportService carbonReportService;

	@Autowired
	private EntityManager entityManager;

	private User owner;
	private User applicant;

	@BeforeEach
	void setUp() {
		// 리포트는 DB 전체를 집계하므로, 로컬 샘플 데이터 등 기존 거래·캠페인을 지우고 시작한다 (테스트 끝나면 롤백)
		for (String entity : new String[] {"Reservation", "Application", "ItemImage", "ItemTradeMethod", "Item", "Campaign"}) {
			entityManager.createQuery("delete from " + entity).executeUpdate();
		}
		owner = persist(user("등록자"));
		applicant = persist(user("신청자"));
	}

	@Test
	void 전체_범위는_거래_완료된_물품만_세고_카테고리는_5개_고정_순서로_0도_채운다() {
		completed(item(CategoryGroup.FURNITURE, 30, null), at(2026, 9, 1));
		completed(item(CategoryGroup.FURNITURE, 30, null), at(2026, 9, 2));
		completed(item(CategoryGroup.APPLIANCE, 24, null), at(2026, 9, 3));
		trade(item(CategoryGroup.KITCHEN, 10, null), ReservationStatus.SCHEDULED, null);
		flushAndClear();

		CarbonReportResponse report = carbonReportService.getReport(ReportScope.ALL, null, TODAY);

		assertThat(report.scope()).isEqualTo(ReportScope.ALL);
		assertThat(report.reusedCount()).isEqualTo(3);
		assertThat(report.carbonReductionKg()).isEqualTo(84);
		assertThat(report.categoryBreakdown())
				.extracting(CategoryCarbon::categoryGroup, CategoryCarbon::carbonReductionKg, CategoryCarbon::ratio)
				.containsExactly(
						tuple(CategoryGroup.FURNITURE, 60L, 0.71),
						tuple(CategoryGroup.APPLIANCE, 24L, 0.29),
						tuple(CategoryGroup.KITCHEN, 0L, 0.0),
						tuple(CategoryGroup.LIVING, 0L, 0.0),
						tuple(CategoryGroup.ETC, 0L, 0.0));
		assertThat(report.campaign()).isNull();
		assertThat(report.dailyTrend()).isEmpty();
		assertThat(report.myCarbonReductionKg()).isNull();
	}

	@Test
	void 전체_범위의_시작_월은_가장_처음_거래가_완료된_달이다() {
		completed(item(CategoryGroup.FURNITURE, 30, null), at(2026, 3, 15));
		completed(item(CategoryGroup.LIVING, 15, null), at(2026, 5, 31));
		completed(item(CategoryGroup.ETC, 8, null), at(2026, 10, 5));
		flushAndClear();

		CarbonReportResponse report = carbonReportService.getReport(ReportScope.ALL, null, TODAY);

		assertThat(report.since()).isEqualTo("2026-03");
		assertThat(report.reusedCount()).isEqualTo(3);
		assertThat(report.carbonReductionKg()).isEqualTo(53);
	}

	@Test
	void 캠페인_범위는_그_캠페인_물품만_세고_시작일부터_오늘까지_날짜별_거래_수를_채운다() {
		Campaign campaign = persist(campaign(TODAY.minusDays(3), TODAY.plusDays(5)));
		completed(item(CategoryGroup.FURNITURE, 30, campaign), TODAY.minusDays(3).atTime(10, 0));
		completed(item(CategoryGroup.FURNITURE, 30, campaign), TODAY.minusDays(1).atTime(10, 0));
		completed(item(CategoryGroup.APPLIANCE, 24, campaign), TODAY.minusDays(1).atTime(18, 0));
		completed(item(CategoryGroup.KITCHEN, 10, null), TODAY.minusDays(1).atTime(12, 0)); // 직거래 — 캠페인 아님
		flushAndClear();

		CarbonReportResponse report = carbonReportService.getReport(ReportScope.CAMPAIGN, null, TODAY);

		assertThat(report.reusedCount()).isEqualTo(3);
		assertThat(report.carbonReductionKg()).isEqualTo(84);
		assertThat(report.campaign().id()).isEqualTo(campaign.getId());
		assertThat(report.campaign().status()).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(report.campaign().startDate()).isEqualTo(TODAY.minusDays(3));
		assertThat(report.campaign().endDate()).isEqualTo(TODAY.plusDays(5));
		assertThat(report.since()).isNull(); // 시작 월은 전체 범위에서만
		assertThat(report.dailyTrend())
				.extracting(DailyTrade::date, DailyTrade::count)
				.containsExactly(
						tuple(TODAY.minusDays(3), 1L),
						tuple(TODAY.minusDays(2), 0L),
						tuple(TODAY.minusDays(1), 2L),
						tuple(TODAY, 0L));
		assertThat(report.categoryBreakdown()).extracting(CategoryCarbon::carbonReductionKg)
				.containsExactly(60L, 24L, 0L, 0L, 0L);
	}

	@Test
	void 진행_중이거나_예정인_캠페인이_없으면_전부_0이고_에러가_아니다() {
		completed(item(CategoryGroup.FURNITURE, 30, null), at(2026, 9, 1));
		flushAndClear();

		CarbonReportResponse report = carbonReportService.getReport(ReportScope.CAMPAIGN, null, TODAY);

		assertThat(report.scope()).isEqualTo(ReportScope.CAMPAIGN);
		assertThat(report.reusedCount()).isZero();
		assertThat(report.carbonReductionKg()).isZero();
		assertThat(report.since()).isNull();
		assertThat(report.campaign()).isNull();
		assertThat(report.dailyTrend()).isEmpty();
		assertThat(report.myCarbonReductionKg()).isNull();
		assertThat(report.categoryBreakdown()).hasSize(5)
				.allSatisfy(category -> assertThat(category.carbonReductionKg()).isZero());
	}

	@Test
	void 시작_전인_예정_캠페인은_날짜별_거래가_빈_배열이다() {
		persist(campaign(TODAY.plusDays(10), TODAY.plusDays(20)));
		flushAndClear();

		CarbonReportResponse report = carbonReportService.getReport(ReportScope.CAMPAIGN, null, TODAY);

		assertThat(report.campaign().status()).isEqualTo(CampaignStatus.PLANNED);
		assertThat(report.dailyTrend()).isEmpty();
	}

	@Test
	void 내가_키운_탄소는_내가_등록해서_거래_완료된_물품만_같은_범위로_센다() {
		Campaign campaign = persist(campaign(TODAY.minusDays(3), TODAY.plusDays(5)));
		completed(item(CategoryGroup.FURNITURE, 30, campaign), TODAY.minusDays(1).atStartOfDay()); // 등록자가 나눔 (캠페인)
		completed(item(CategoryGroup.APPLIANCE, 24, null), at(2026, 9, 1)); // 등록자가 나눔 (직거래)
		flushAndClear();

		assertThat(carbonReportService.getReport(ReportScope.ALL, owner.getId(), TODAY).myCarbonReductionKg())
				.isEqualTo(54);
		assertThat(carbonReportService.getReport(ReportScope.CAMPAIGN, owner.getId(), TODAY).myCarbonReductionKg())
				.isEqualTo(30);
		// 받은 쪽은 세지 않는다 — 한 거래가 두 사람에게 중복으로 잡히지 않게 (동네 전체 = 각자 키운 나무의 합)
		assertThat(carbonReportService.getReport(ReportScope.ALL, applicant.getId(), TODAY).myCarbonReductionKg())
				.isZero();
	}

	@Test
	void 캠페인이_없어도_로그인했으면_내가_키운_탄소는_0이다() {
		assertThat(carbonReportService.getReport(ReportScope.CAMPAIGN, owner.getId(), TODAY).myCarbonReductionKg())
				.isZero();
	}

	@Test
	void 거래가_하나도_없으면_시작_월은_null이고_비율은_0이다() {
		CarbonReportResponse report = carbonReportService.getReport(ReportScope.ALL, null, TODAY);

		assertThat(report.reusedCount()).isZero();
		assertThat(report.since()).isNull();
		assertThat(report.categoryBreakdown()).extracting(CategoryCarbon::ratio).containsOnly(0.0);
	}

	private Item item(CategoryGroup group, int carbonKg, Campaign campaign) {
		return persist(Item.builder()
				.owner(owner)
				.campaign(campaign)
				.name("물품")
				.categoryGroup(group)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(carbonKg)
				.availableFrom(TODAY)
				.applicationDeadline(TODAY.atStartOfDay())
				.status(ItemStatus.COMPLETED)
				.build());
	}

	private void completed(Item item, LocalDateTime completedAt) {
		trade(item, ReservationStatus.COMPLETED, completedAt);
	}

	private void trade(Item item, ReservationStatus status, LocalDateTime completedAt) {
		Application application = persist(Application.builder()
				.item(item).applicant(applicant).priorityScore(0).status(ApplicationStatus.SELECTED).build());
		persist(Reservation.builder()
				.application(application).tradeMethod(TradeMethod.DIRECT).status(status).completedAt(completedAt).build());
	}

	private static LocalDateTime at(int year, int month, int day) {
		return LocalDateTime.of(year, month, day, 12, 0);
	}

	private static Campaign campaign(LocalDate start, LocalDate end) {
		return Campaign.builder()
				.name("테스트 캠페인")
				.registrationStartDate(start)
				.registrationEndDate(end)
				.applicationStartDate(start)
				.applicationEndDate(end)
				.pickupStartDate(start)
				.pickupEndDate(end)
				.locationName("거점")
				.locationAddress("서울 노원구 광운로 20")
				.status(CampaignStatus.ACTIVE)
				.build();
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private static User user(String nickname) {
		return User.builder().kakaoId("test-" + UUID.randomUUID()).nickname(nickname).build();
	}

}
