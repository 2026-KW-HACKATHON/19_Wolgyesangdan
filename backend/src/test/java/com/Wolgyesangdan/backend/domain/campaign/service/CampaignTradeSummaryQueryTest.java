package com.Wolgyesangdan.backend.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
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
 * 실제 MySQL에서 캠페인별 거래 집계 확인 — 한 직거래가 두 캠페인에 잡히지 않아야 한다 (#250). 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, CampaignService.class})
class CampaignTradeSummaryQueryTest {

	private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

	@Autowired
	private CampaignService campaignService;

	@Autowired
	private EntityManager entityManager;

	private User owner;
	private User applicant;

	@BeforeEach
	void setUp() {
		// 집계는 DB 전체를 보므로, 로컬 샘플 데이터 등 기존 거래·캠페인을 지우고 시작한다 (테스트 끝나면 롤백)
		for (String entity : new String[] {"Reservation", "Application", "ItemImage", "ItemTradeMethod", "Item", "Campaign"}) {
			entityManager.createQuery("delete from " + entity).executeUpdate();
		}
		owner = persist(user("등록자"));
		applicant = persist(user("신청자"));
	}

	@Test
	void 기간이_겹치는_두_캠페인은_다음_캠페인이_시작한_뒤의_직거래를_이전_캠페인에_세지_않는다() {
		// 운영 중을 일찍 끈 예전 캠페인 — 끈 시각 기록이 없다(이 기능 전에 끈 캠페인)
		Campaign previous = persist(campaign(DAY.minusDays(10), DAY.plusDays(30), CampaignStatus.ENDED));
		Campaign current = persist(campaign(DAY, DAY.plusDays(20), CampaignStatus.ACTIVE));
		completed(item(null, 30), DAY.minusDays(5).atTime(12, 0)); // 이전 캠페인 때의 직거래
		completed(item(null, 70), DAY.atTime(13, 0));              // 지금 캠페인의 직거래
		completed(item(null, 70), DAY.plusDays(1).atTime(9, 0));   // 지금 캠페인의 직거래
		flushAndClear();

		assertThat(summarize(previous)).isEqualTo(new CompletedItemSummary(1, 30));
		assertThat(summarize(current)).isEqualTo(new CompletedItemSummary(2, 140));
	}

	@Test
	void 운영_중을_끈_캠페인은_끈_시각_뒤의_직거래를_세지_않는다() {
		Campaign closed = persist(campaign(DAY.minusDays(10), DAY.plusDays(30), CampaignStatus.ACTIVE));
		closed.end(DAY.atTime(12, 0));
		completed(item(null, 30), DAY.atTime(11, 59));          // 끄기 직전
		completed(item(null, 70), DAY.atTime(12, 0));           // 끈 시각 — 세지 않는다
		completed(item(null, 70), DAY.plusDays(3).atTime(9, 0)); // 끈 뒤 (다음 캠페인이 아직 없는 기간)
		flushAndClear();

		assertThat(summarize(closed)).isEqualTo(new CompletedItemSummary(1, 30));
	}

	@Test
	void 캠페인에_연결된_물품은_캠페인이_끝난_뒤에_완료돼도_그_캠페인에만_센다() {
		Campaign previous = persist(campaign(DAY.minusDays(10), DAY.plusDays(30), CampaignStatus.ACTIVE));
		previous.end(DAY.minusDays(1).atTime(18, 0));
		Campaign current = persist(campaign(DAY, DAY.plusDays(20), CampaignStatus.ACTIVE));
		completed(item(previous, 24), DAY.plusDays(2).atTime(10, 0)); // 이전 캠페인 물품이 지금 캠페인 기간에 완료
		completed(item(current, 10), DAY.plusDays(2).atTime(11, 0));
		flushAndClear();

		assertThat(summarize(previous)).isEqualTo(new CompletedItemSummary(1, 24));
		assertThat(summarize(current)).isEqualTo(new CompletedItemSummary(1, 10));
	}

	@Test
	void 겹치지_않는_캠페인은_각자_기간의_직거래를_그대로_센다() {
		Campaign spring = persist(campaign(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20), CampaignStatus.ACTIVE));
		Campaign autumn = persist(campaign(DAY, DAY.plusDays(20), CampaignStatus.ACTIVE));
		completed(item(null, 30), LocalDateTime.of(2026, 3, 20, 23, 59)); // 봄 캠페인 종료일 마지막
		completed(item(null, 15), LocalDateTime.of(2026, 6, 1, 12, 0));   // 어느 캠페인도 아닌 기간
		completed(item(null, 70), DAY.atTime(13, 0));
		flushAndClear();

		assertThat(summarize(spring)).isEqualTo(new CompletedItemSummary(1, 30));
		assertThat(summarize(autumn)).isEqualTo(new CompletedItemSummary(1, 70));
	}

	private CompletedItemSummary summarize(Campaign campaign) {
		return campaignService.summarizeTrades(entityManager.find(Campaign.class, campaign.getId()));
	}

	private Item item(Campaign campaign, int carbonKg) {
		return persist(Item.builder()
				.owner(owner)
				.campaign(campaign)
				.name("물품")
				.categoryGroup(CategoryGroup.APPLIANCE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(carbonKg)
				.applicationDeadline(DAY.atStartOfDay())
				.status(ItemStatus.COMPLETED)
				.build());
	}

	private void completed(Item item, LocalDateTime completedAt) {
		Application application = persist(Application.builder()
				.item(item).applicant(applicant).priorityScore(0).status(ApplicationStatus.COMPLETED).build());
		persist(Reservation.builder()
				.application(application)
				.tradeMethod(item.getCampaign() == null ? TradeMethod.DIRECT : TradeMethod.CAMPAIGN)
				.status(ReservationStatus.COMPLETED)
				.completedAt(completedAt)
				.build());
	}

	private static Campaign campaign(LocalDate start, LocalDate end, CampaignStatus status) {
		return Campaign.builder()
				.name("테스트 캠페인")
				.registrationStartDate(start).registrationEndDate(end)
				.applicationStartDate(start).applicationEndDate(end)
				.pickupStartDate(start).pickupEndDate(end)
				.locationName("거점").locationAddress("서울 노원구 광운로 20")
				.status(status)
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
