package com.Wolgyesangdan.backend.domain.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;
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
 * 실제 MySQL에서 신청 마감 → 배정 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, AssignmentService.class})
class AssignmentServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 10, 12, 0);

	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private EntityManager entityManager;

	private User owner;

	@BeforeEach
	void setUp() {
		owner = persist(user("등록자"));
	}

	@Test
	void 마감이_지나면_우선배정_점수가_높은_신청을_배정하고_예약을_만든다() {
		Item item = persist(item(ItemStatus.OPEN, NOW.minusMinutes(1)));
		Application first = apply(item, 0);   // 먼저 신청했지만 0점
		Application prioritized = apply(item, 1);
		flushAndClear();

		assertThat(assignmentService.assign(item.getId(), NOW)).isEqualTo(AssignmentService.Result.ASSIGNED);
		flushAndClear();

		assertThat(find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.ASSIGNED);
		Application selected = find(Application.class, prioritized.getId());
		assertThat(selected.getStatus()).isEqualTo(ApplicationStatus.SELECTED);
		assertThat(selected.getSelectedAt()).isEqualTo(NOW);
		assertThat(find(Application.class, first.getId()).getStatus()).isEqualTo(ApplicationStatus.WAITING);

		Reservation reservation = reservationOf(prioritized);
		assertThat(reservation.getTradeMethod()).isEqualTo(TradeMethod.DIRECT);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.SCHEDULED);
		assertThat(reservation.getReconfirmationDeadline()).isEqualTo(NOW.plusHours(24));
		assertThat(reservation.getScheduledAt()).isNull();
	}

	@Test
	void 점수가_같으면_먼저_신청한_사람을_배정한다() {
		Item item = persist(item(ItemStatus.OPEN, NOW.minusMinutes(1)));
		Application first = apply(item, 1);
		apply(item, 1);
		flushAndClear();

		assignmentService.assign(item.getId(), NOW);
		flushAndClear();

		assertThat(find(Application.class, first.getId()).getStatus()).isEqualTo(ApplicationStatus.SELECTED);
	}

	@Test
	void 거점_거래를_포함한_물품은_거점_예약으로_시작한다() {
		Item item = persist(item(ItemStatus.OPEN, NOW.minusMinutes(1)));
		persist(ItemTradeMethod.builder().item(item).tradeMethod(TradeMethod.DIRECT).build());
		persist(ItemTradeMethod.builder().item(item).tradeMethod(TradeMethod.CAMPAIGN).build());
		Application application = apply(item, 0);
		flushAndClear();

		assignmentService.assign(item.getId(), NOW);
		flushAndClear();

		Reservation reservation = reservationOf(application);
		assertThat(reservation.getTradeMethod()).isEqualTo(TradeMethod.CAMPAIGN);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HUB_DROP_SCHEDULED);
	}

	@Test
	void 정원이_차서_마감된_물품도_배정한다() {
		Item item = persist(item(ItemStatus.CLOSED, NOW.minusMinutes(1)));
		Application application = apply(item, 0);
		flushAndClear();

		assertThat(assignmentService.assign(item.getId(), NOW)).isEqualTo(AssignmentService.Result.ASSIGNED);
		flushAndClear();

		assertThat(find(Application.class, application.getId()).getStatus()).isEqualTo(ApplicationStatus.SELECTED);
	}

	@Test
	void 신청자가_없으면_물품을_종료한다() {
		Item item = persist(item(ItemStatus.OPEN, NOW.minusMinutes(1)));
		flushAndClear();

		assertThat(assignmentService.assign(item.getId(), NOW)).isEqualTo(AssignmentService.Result.CANCELED);
		flushAndClear();

		assertThat(find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.CANCELED);
	}

	@Test
	void 취소한_신청만_있으면_신청자가_없는_것으로_본다() {
		Item item = persist(item(ItemStatus.OPEN, NOW.minusMinutes(1)));
		Application canceled = apply(item, 1, ApplicationStatus.CANCELED);
		flushAndClear();

		assertThat(assignmentService.assign(item.getId(), NOW)).isEqualTo(AssignmentService.Result.CANCELED);
		flushAndClear();

		assertThat(find(Application.class, canceled.getId()).getStatus()).isEqualTo(ApplicationStatus.CANCELED);
		assertThat(reservationCount(canceled)).isZero();
	}

	@Test
	void 마감_전이면_건너뛴다() {
		Item item = persist(item(ItemStatus.OPEN, NOW.plusSeconds(1)));
		Application application = apply(item, 0);
		flushAndClear();

		assertThat(assignmentService.assign(item.getId(), NOW)).isEqualTo(AssignmentService.Result.SKIPPED);
		flushAndClear();

		assertThat(find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.OPEN);
		assertThat(find(Application.class, application.getId()).getStatus()).isEqualTo(ApplicationStatus.WAITING);
	}

	@Test
	void 같은_물품을_두_번_처리해도_한_번만_배정된다() {
		Item item = persist(item(ItemStatus.OPEN, NOW.minusMinutes(1)));
		Application first = apply(item, 0);
		Application second = apply(item, 0);
		flushAndClear();

		assignmentService.assign(item.getId(), NOW);
		flushAndClear();
		assertThat(assignmentService.assign(item.getId(), NOW.plusMinutes(1)))
				.isEqualTo(AssignmentService.Result.SKIPPED);
		flushAndClear();

		assertThat(reservationCount(first)).isEqualTo(1);
		assertThat(reservationCount(second)).isZero();
		assertThat(find(Application.class, second.getId()).getStatus()).isEqualTo(ApplicationStatus.WAITING);
	}

	@Test
	void 없는_물품이면_건너뛴다() {
		assertThat(assignmentService.assign(Long.MAX_VALUE, NOW)).isEqualTo(AssignmentService.Result.SKIPPED);
	}

	@Test
	void 마감이_지난_신청_받는_중_또는_정원_마감_물품만_대상으로_고른다() {
		Item open = persist(item(ItemStatus.OPEN, NOW.minusMinutes(2)));
		Item closed = persist(item(ItemStatus.CLOSED, NOW.minusMinutes(1)));
		Item notYet = persist(item(ItemStatus.OPEN, NOW.plusMinutes(1)));
		Item assigned = persist(item(ItemStatus.ASSIGNED, NOW.minusMinutes(3)));
		Item canceled = persist(item(ItemStatus.CANCELED, NOW.minusMinutes(3)));
		flushAndClear();

		List<Long> ids = assignmentService.findItemIdsToAssign(NOW);

		assertThat(ids).containsSubsequence(open.getId(), closed.getId());
		assertThat(ids).doesNotContain(notYet.getId(), assigned.getId(), canceled.getId());
	}

	private Application apply(Item item, int priorityScore) {
		return apply(item, priorityScore, ApplicationStatus.WAITING);
	}

	private Application apply(Item item, int priorityScore, ApplicationStatus status) {
		Application application = persist(Application.builder()
				.item(item).applicant(persist(user("신청자"))).priorityScore(priorityScore).status(status).build());
		entityManager.flush(); // 신청 시각(created_at) 순서를 확실히 남긴다
		return application;
	}

	private Reservation reservationOf(Application application) {
		return entityManager.createQuery("select r from Reservation r where r.application.id = :id", Reservation.class)
				.setParameter("id", application.getId())
				.getSingleResult();
	}

	private long reservationCount(Application application) {
		return entityManager.createQuery("select count(r) from Reservation r where r.application.id = :id", Long.class)
				.setParameter("id", application.getId())
				.getSingleResult();
	}

	private <T> T find(Class<T> type, Long id) {
		return entityManager.find(type, id);
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

	private Item item(ItemStatus status, LocalDateTime applicationDeadline) {
		return Item.builder()
				.owner(owner)
				.name("1인용 책상")
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(30)
				.availableFrom(LocalDate.now())
				.applicationDeadline(applicationDeadline)
				.status(status)
				.build();
	}

}
