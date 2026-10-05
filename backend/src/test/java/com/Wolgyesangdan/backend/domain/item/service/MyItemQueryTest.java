package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.dto.MyItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

/**
 * 실제 MySQL에서 내가 등록한 물품 목록 확인. 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ItemService.class})
class MyItemQueryTest {

	private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 10, 8, 14, 0);

	@Autowired
	private ItemService itemService;

	@Autowired
	private EntityManager entityManager;

	private User owner;

	@BeforeEach
	void setUp() {
		owner = persist(user("등록자"));
	}

	@Test
	void 내_물품만_상태와_관계없이_최근_등록순으로_내려준다() {
		Item canceled = persist(item(owner, "취소된 물품", ItemStatus.CANCELED));
		Item open = persist(item(owner, "모집 중 물품", ItemStatus.OPEN));
		Item completed = persist(item(owner, "완료된 물품", ItemStatus.COMPLETED));
		persist(item(persist(user("다른 등록자")), "남의 물품", ItemStatus.OPEN));
		flushAndClear();

		Page<MyItemSummaryResponse> result = itemService.getMyItems(owner.getId(), 0, 20);

		// 같은 시각에 저장될 수 있어 id 역순이 두 번째 정렬 기준
		assertThat(result.getContent()).extracting(MyItemSummaryResponse::id)
				.containsExactly(completed.getId(), open.getId(), canceled.getId());
		assertThat(result.getTotalElements()).isEqualTo(3);
	}

	@Test
	void 대표_사진은_순서가_가장_앞인_사진이고_없으면_null() {
		Item withImages = persist(item(owner, "사진 있음", ItemStatus.OPEN));
		persist(ItemImage.builder().item(withImages).imageUrl("second.jpg").displayOrder(1).build());
		persist(ItemImage.builder().item(withImages).imageUrl("first.jpg").displayOrder(0).build());
		Item withoutImages = persist(item(owner, "사진 없음", ItemStatus.OPEN));
		flushAndClear();

		Page<MyItemSummaryResponse> result = itemService.getMyItems(owner.getId(), 0, 20);

		assertThat(find(result, withImages).thumbnailImageUrl()).isEqualTo("first.jpg");
		assertThat(find(result, withoutImages).thumbnailImageUrl()).isNull();
	}

	@Test
	void 배정된_신청_id와_전달_예정_일시는_배정_이후_물품의_진행_중인_예약에서_가져온다() {
		Item assigned = persist(item(owner, "배정됨", ItemStatus.ASSIGNED));
		Application assignedApplication = reservation(assigned, ReservationStatus.SCHEDULED, SCHEDULED_AT);
		Item completed = persist(item(owner, "완료", ItemStatus.COMPLETED));
		Application completedApplication = reservation(completed, ReservationStatus.COMPLETED,
				SCHEDULED_AT.minusDays(1));
		Item open = persist(item(owner, "모집 중", ItemStatus.OPEN));
		flushAndClear();

		Page<MyItemSummaryResponse> result = itemService.getMyItems(owner.getId(), 0, 20);

		assertThat(find(result, assigned).applicationId()).isEqualTo(assignedApplication.getId());
		assertThat(find(result, assigned).scheduledAt()).isEqualTo(SCHEDULED_AT);
		assertThat(find(result, completed).applicationId()).isEqualTo(completedApplication.getId());
		assertThat(find(result, completed).scheduledAt()).isEqualTo(SCHEDULED_AT.minusDays(1));
		assertThat(find(result, open).applicationId()).isNull();
		assertThat(find(result, open).scheduledAt()).isNull();
	}

	@Test
	void 취소된_물품은_예약이_남아_있어도_신청_id를_내려주지_않는다() {
		// 승계할 대기자가 없어 물품이 취소된 경우 등 — 예약 행은 남아 있어도 진행 중인 거래가 아니다
		Item canceled = persist(item(owner, "취소됨", ItemStatus.CANCELED));
		reservation(canceled, ReservationStatus.SCHEDULED, SCHEDULED_AT);
		flushAndClear();

		MyItemSummaryResponse summary = find(itemService.getMyItems(owner.getId(), 0, 20), canceled);

		assertThat(summary.applicationId()).isNull();
		assertThat(summary.scheduledAt()).isNull();
	}

	@Test
	void 노쇼로_다음_대기자에게_넘어갔으면_새_예약의_신청_id와_일시를_쓴다() {
		Item item = persist(item(owner, "승계됨", ItemStatus.ASSIGNED));
		reservation(item, ReservationStatus.NO_SHOW, SCHEDULED_AT.minusDays(2));
		Application successor = reservation(item, ReservationStatus.SCHEDULED, SCHEDULED_AT);
		flushAndClear();

		MyItemSummaryResponse summary = find(itemService.getMyItems(owner.getId(), 0, 20), item);

		assertThat(summary.applicationId()).isEqualTo(successor.getId());
		assertThat(summary.scheduledAt()).isEqualTo(SCHEDULED_AT);
	}

	@Test
	void 등록한_물품이_없으면_빈_페이지() {
		Page<MyItemSummaryResponse> result = itemService.getMyItems(owner.getId(), 0, 20);

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}

	@Test
	void 쿼리_수는_물품_수와_관계없이_고정된다() {
		// 한 페이지에 다 안 들어가게 만들어 count 쿼리까지 나가게 한다
		for (int i = 0; i < 10; i++) {
			Item item = persist(item(owner, "물품" + i, ItemStatus.ASSIGNED));
			persist(ItemImage.builder().item(item).imageUrl("photo" + i + ".jpg").displayOrder(0).build());
			reservation(item, ReservationStatus.SCHEDULED, SCHEDULED_AT);
		}
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		itemService.getMyItems(owner.getId(), 0, 5);

		// 물품 페이지, 전체 개수, 대표 사진, 진행 중인 예약(신청 id·일시)
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
	}

	private static MyItemSummaryResponse find(Page<MyItemSummaryResponse> page, Item item) {
		return page.getContent().stream().filter(summary -> summary.id().equals(item.getId())).findFirst().orElseThrow();
	}

	private Application reservation(Item item, ReservationStatus status, LocalDateTime scheduledAt) {
		// (item, applicant)가 유니크라 예약마다 신청자를 따로 만든다
		Application application = persist(Application.builder()
				.item(item).applicant(persist(user("신청자"))).priorityScore(0).status(ApplicationStatus.SELECTED).build());
		persist(Reservation.builder()
				.application(application).tradeMethod(TradeMethod.DIRECT).scheduledAt(scheduledAt).status(status).build());
		return application;
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

	private static Item item(User owner, String name, ItemStatus status) {
		return Item.builder()
				.owner(owner)
				.name(name)
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(30)
				.availableFrom(LocalDate.now())
				.applicationDeadline(LocalDateTime.now().plusDays(3))
				.status(status)
				.build();
	}

}
