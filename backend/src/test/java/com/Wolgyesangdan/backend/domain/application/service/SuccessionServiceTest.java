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
 * 실제 MySQL에서 재확인 기한 초과·노쇼 승계 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, SuccessionService.class})
class SuccessionServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 10, 12, 0);

	@Autowired
	private SuccessionService successionService;

	@Autowired
	private EntityManager entityManager;

	private Item item;

	@BeforeEach
	void setUp() {
		item = persist(Item.builder()
				.owner(persist(user("등록자")))
				.name("1인용 책상")
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(30)
				.availableFrom(LocalDate.now())
				.applicationDeadline(NOW.minusDays(2))
				.status(ItemStatus.ASSIGNED)
				.build());
	}

	@Test
	void 재확인_기한이_지나면_노쇼_처리하고_다음_대기자에게_넘긴다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		Application next = apply(0, ApplicationStatus.WAITING);
		Reservation expired = reserve(selected, TradeMethod.DIRECT, ReservationStatus.SCHEDULED, NOW.minusSeconds(1));
		flushAndClear();

		assertThat(successionService.succeed(expired.getId(), NOW)).isEqualTo(SuccessionService.Result.SUCCEEDED);
		flushAndClear();

		assertThat(find(Reservation.class, expired.getId()).getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
		assertThat(find(Application.class, selected.getId()).getStatus()).isEqualTo(ApplicationStatus.CANCELED);
		Application successor = find(Application.class, next.getId());
		assertThat(successor.getStatus()).isEqualTo(ApplicationStatus.SELECTED);
		assertThat(successor.getSelectedAt()).isEqualTo(NOW);
		Reservation reservation = reservationOf(next);
		assertThat(reservation.getTradeMethod()).isEqualTo(TradeMethod.DIRECT);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.SCHEDULED);
		assertThat(reservation.getReconfirmationDeadline()).isEqualTo(NOW.plusHours(24));
		assertThat(find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.ASSIGNED);
	}

	@Test
	void 다음_대기자는_우선배정_점수가_높은_사람() {
		Application selected = apply(1, ApplicationStatus.SELECTED);
		Application early = apply(0, ApplicationStatus.WAITING);
		Application prioritized = apply(1, ApplicationStatus.WAITING);
		Reservation expired = reserve(selected, TradeMethod.DIRECT, ReservationStatus.SCHEDULED, NOW.minusSeconds(1));
		flushAndClear();

		successionService.succeed(expired.getId(), NOW);
		flushAndClear();

		assertThat(find(Application.class, prioritized.getId()).getStatus()).isEqualTo(ApplicationStatus.SELECTED);
		assertThat(find(Application.class, early.getId()).getStatus()).isEqualTo(ApplicationStatus.WAITING);
	}

	@Test
	void 운영진이_노쇼로_표시한_예약도_재확인_여부와_관계없이_넘긴다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		Application next = apply(0, ApplicationStatus.WAITING);
		Reservation noShow = reserve(selected, TradeMethod.DIRECT, ReservationStatus.NO_SHOW, NOW.plusDays(1));
		flushAndClear();

		assertThat(successionService.succeed(noShow.getId(), NOW)).isEqualTo(SuccessionService.Result.SUCCEEDED);
		flushAndClear();

		assertThat(find(Application.class, selected.getId()).getStatus()).isEqualTo(ApplicationStatus.CANCELED);
		assertThat(find(Application.class, next.getId()).getStatus()).isEqualTo(ApplicationStatus.SELECTED);
	}

	@Test
	void 넘길_대기자가_없으면_물품을_종료한다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		apply(1, ApplicationStatus.CANCELED); // 취소한 신청은 대기자가 아니다
		Reservation expired = reserve(selected, TradeMethod.DIRECT, ReservationStatus.SCHEDULED, NOW.minusSeconds(1));
		flushAndClear();

		assertThat(successionService.succeed(expired.getId(), NOW)).isEqualTo(SuccessionService.Result.CANCELED);
		flushAndClear();

		assertThat(find(Reservation.class, expired.getId()).getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
		assertThat(find(Application.class, selected.getId()).getStatus()).isEqualTo(ApplicationStatus.CANCELED);
		assertThat(find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.CANCELED);
	}

	@Test
	void 거점_맡기기_전에_노쇼면_다음_사람도_맡기기_예정부터_시작한다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		Application next = apply(0, ApplicationStatus.WAITING);
		Reservation expired = reserve(selected, TradeMethod.CAMPAIGN, ReservationStatus.HUB_DROP_SCHEDULED,
				NOW.minusSeconds(1));
		flushAndClear();

		successionService.succeed(expired.getId(), NOW);
		flushAndClear();

		Reservation reservation = reservationOf(next);
		assertThat(reservation.getTradeMethod()).isEqualTo(TradeMethod.CAMPAIGN);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HUB_DROP_SCHEDULED);
	}

	@Test
	void 물품이_이미_거점에_있으면_다음_사람은_바로_수령_예정이다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		Application next = apply(0, ApplicationStatus.WAITING);
		Reservation expired = reserve(selected, TradeMethod.CAMPAIGN, ReservationStatus.HUB_RECEIVED,
				NOW.minusSeconds(1));
		flushAndClear();

		successionService.succeed(expired.getId(), NOW);
		flushAndClear();

		assertThat(reservationOf(next).getStatus()).isEqualTo(ReservationStatus.PICKUP_SCHEDULED);
	}

	@Test
	void 재확인한_예약은_기한이_지나도_넘기지_않는다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		Application next = apply(0, ApplicationStatus.WAITING);
		Reservation reconfirmed = reserve(selected, TradeMethod.DIRECT, ReservationStatus.RECONFIRMED,
				NOW.minusDays(1));
		flushAndClear();

		assertThat(successionService.succeed(reconfirmed.getId(), NOW)).isEqualTo(SuccessionService.Result.SKIPPED);
		flushAndClear();

		assertThat(find(Reservation.class, reconfirmed.getId()).getStatus()).isEqualTo(ReservationStatus.RECONFIRMED);
		assertThat(find(Application.class, next.getId()).getStatus()).isEqualTo(ApplicationStatus.WAITING);
	}

	@Test
	void 재확인_기한_시각_정각까지는_넘기지_않는다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		Reservation reservation = reserve(selected, TradeMethod.DIRECT, ReservationStatus.SCHEDULED, NOW);
		flushAndClear();

		assertThat(successionService.succeed(reservation.getId(), NOW)).isEqualTo(SuccessionService.Result.SKIPPED);
	}

	@Test
	void 같은_예약을_두_번_처리해도_한_번만_넘긴다() {
		Application selected = apply(0, ApplicationStatus.SELECTED);
		Application second = apply(0, ApplicationStatus.WAITING);
		Application third = apply(0, ApplicationStatus.WAITING);
		Reservation expired = reserve(selected, TradeMethod.DIRECT, ReservationStatus.SCHEDULED, NOW.minusSeconds(1));
		flushAndClear();

		successionService.succeed(expired.getId(), NOW);
		flushAndClear();
		assertThat(successionService.succeed(expired.getId(), NOW.plusMinutes(1)))
				.isEqualTo(SuccessionService.Result.SKIPPED);
		flushAndClear();

		assertThat(find(Application.class, second.getId()).getStatus()).isEqualTo(ApplicationStatus.SELECTED);
		assertThat(find(Application.class, third.getId()).getStatus()).isEqualTo(ApplicationStatus.WAITING);
	}

	@Test
	void 없는_예약이면_건너뛴다() {
		assertThat(successionService.succeed(Long.MAX_VALUE, NOW)).isEqualTo(SuccessionService.Result.SKIPPED);
	}

	@Test
	void 기한이_지난_미확인_예약과_승계_전_노쇼_예약만_대상으로_고른다() {
		Reservation expired = reserve(apply(0, ApplicationStatus.SELECTED), TradeMethod.DIRECT,
				ReservationStatus.SCHEDULED, NOW.minusSeconds(1));
		Reservation noShow = reserve(apply(0, ApplicationStatus.SELECTED), TradeMethod.CAMPAIGN,
				ReservationStatus.NO_SHOW, NOW.plusDays(1));
		Reservation notYet = reserve(apply(0, ApplicationStatus.SELECTED), TradeMethod.DIRECT,
				ReservationStatus.SCHEDULED, NOW.plusSeconds(1));
		Reservation reconfirmed = reserve(apply(0, ApplicationStatus.SELECTED), TradeMethod.DIRECT,
				ReservationStatus.RECONFIRMED, NOW.minusDays(1));
		Reservation alreadySucceeded = reserve(apply(0, ApplicationStatus.CANCELED), TradeMethod.DIRECT,
				ReservationStatus.NO_SHOW, NOW.minusDays(1));
		flushAndClear();

		List<Long> ids = successionService.findReservationIdsToSucceed(NOW);

		assertThat(ids).contains(expired.getId(), noShow.getId());
		assertThat(ids).doesNotContain(notYet.getId(), reconfirmed.getId(), alreadySucceeded.getId());
	}

	private Application apply(int priorityScore, ApplicationStatus status) {
		Application application = persist(Application.builder()
				.item(item).applicant(persist(user("신청자"))).priorityScore(priorityScore).status(status).build());
		entityManager.flush(); // 신청 시각(created_at) 순서를 확실히 남긴다
		return application;
	}

	private Reservation reserve(Application application, TradeMethod tradeMethod, ReservationStatus status,
			LocalDateTime reconfirmationDeadline) {
		return persist(Reservation.builder()
				.application(application)
				.tradeMethod(tradeMethod)
				.status(status)
				.reconfirmationDeadline(reconfirmationDeadline)
				.build());
	}

	private Reservation reservationOf(Application application) {
		return entityManager.createQuery("select r from Reservation r where r.application.id = :id", Reservation.class)
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

}
