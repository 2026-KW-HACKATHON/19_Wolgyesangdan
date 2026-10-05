package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.item.dto.ItemDetailResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 물품 상세 조회 확인. 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ItemService.class})
class ItemDetailQueryTest {

	@Autowired
	private ItemService itemService;

	@Autowired
	private EntityManager entityManager;

	private User owner;
	private User applicant;

	@BeforeEach
	void setUp() {
		owner = persist(user("등록자"));
		applicant = persist(user("신청자"));
	}

	@Test
	void 거점_거래_물품은_사진_순서대로_거래_방식_캠페인_정보까지_내려준다() {
		Campaign campaign = persist(campaign());
		Item item = persist(item(owner, campaign, ItemStatus.OPEN));
		persist(ItemImage.builder().item(item).imageUrl("second.jpg").displayOrder(1).build());
		persist(ItemImage.builder().item(item).imageUrl("first.jpg").displayOrder(0).build());
		persist(ItemTradeMethod.builder().item(item).tradeMethod(TradeMethod.CAMPAIGN).build());
		persist(ItemTradeMethod.builder().item(item).tradeMethod(TradeMethod.DIRECT).build());
		flushAndClear();

		ItemDetailResponse response = itemService.getItem(item.getId());

		assertThat(response.name()).isEqualTo("전자레인지");
		assertThat(response.description()).isEqualTo("이사 때문에 내놓아요.");
		assertThat(response.maxApplicants()).isEqualTo(Item.MAX_APPLICANTS);
		assertThat(response.tradeMethods()).containsExactly(TradeMethod.DIRECT, TradeMethod.CAMPAIGN);
		assertThat(response.images()).extracting(ItemDetailResponse.ImageResponse::imageUrl)
				.containsExactly("first.jpg", "second.jpg");
		assertThat(response.campaign().id()).isEqualTo(campaign.getId());
		assertThat(response.campaign().locationName()).isEqualTo("광운대 비마관 1층 거점");
		assertThat(response.owner().nickname()).isEqualTo("등록자");
	}

	@Test
	void 직거래_전용_물품은_캠페인이_null이고_사진이_없으면_빈_목록() {
		Item item = persist(item(owner, null, ItemStatus.OPEN));
		flushAndClear();

		ItemDetailResponse response = itemService.getItem(item.getId());

		assertThat(response.campaign()).isNull();
		assertThat(response.images()).isEmpty();
		assertThat(response.tradeMethods()).isEmpty();
	}

	@Test
	void 등록자의_전달_완료_횟수는_그_사람_물품의_완료된_예약만_센다() {
		Item target = persist(item(owner, null, ItemStatus.OPEN));
		reservation(persist(item(owner, null, ItemStatus.COMPLETED)), ReservationStatus.COMPLETED);
		reservation(persist(item(owner, null, ItemStatus.COMPLETED)), ReservationStatus.COMPLETED);
		reservation(persist(item(owner, null, ItemStatus.ASSIGNED)), ReservationStatus.SCHEDULED);   // 진행 중
		User other = persist(user("다른 등록자"));
		reservation(persist(item(other, null, ItemStatus.COMPLETED)), ReservationStatus.COMPLETED);  // 남의 물품
		flushAndClear();

		assertThat(itemService.getItem(target.getId()).owner().givenCount()).isEqualTo(2);
	}

	@Test
	void 쿼리는_4번으로_고정된다() {
		Item item = persist(item(owner, persist(campaign()), ItemStatus.OPEN));
		for (int i = 0; i < 5; i++) {
			persist(ItemImage.builder().item(item).imageUrl("photo" + i + ".jpg").displayOrder(i).build());
		}
		persist(ItemTradeMethod.builder().item(item).tradeMethod(TradeMethod.DIRECT).build());
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		itemService.getItem(item.getId());

		// 물품+등록자+캠페인, 사진, 거래 방식, 등록자 전달 완료 횟수
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
	}

	@Test
	void 없는_물품이면_ITEM_NOT_FOUND() {
		assertThatThrownBy(() -> itemService.getItem(Long.MAX_VALUE))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_FOUND);
	}

	private void reservation(Item item, ReservationStatus status) {
		Application application = persist(Application.builder()
				.item(item).applicant(applicant).priorityScore(0).status(ApplicationStatus.SELECTED).build());
		persist(Reservation.builder()
				.application(application).tradeMethod(TradeMethod.DIRECT).status(status).build());
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

	private static Campaign campaign() {
		LocalDate today = LocalDate.now();
		return Campaign.builder()
				.name("테스트 캠페인")
				.registrationStartDate(today).registrationEndDate(today)
				.applicationStartDate(today).applicationEndDate(today)
				.pickupStartDate(today).pickupEndDate(today)
				.locationName("광운대 비마관 1층 거점").locationAddress("서울 노원구 광운로 20").hubHours("평일 10:00 ~ 18:00")
				.status(CampaignStatus.ACTIVE)
				.build();
	}

	private static Item item(User owner, Campaign campaign, ItemStatus status) {
		return Item.builder()
				.owner(owner)
				.campaign(campaign)
				.name("전자레인지")
				.categoryGroup(CategoryGroup.APPLIANCE)
				.category("생활가전")
				.description("이사 때문에 내놓아요.")
				.conditionGrade("상태 좋음")
				.usagePeriod("2년 사용")
				.transportDifficulty("보통")
				.estimatedCarbonReduction(24)
				.availableFrom(LocalDate.now())
				.availableUntil(LocalDate.now().plusDays(7))
				.applicationDeadline(LocalDateTime.now().plusDays(3))
				.status(status)
				.build();
	}

}
