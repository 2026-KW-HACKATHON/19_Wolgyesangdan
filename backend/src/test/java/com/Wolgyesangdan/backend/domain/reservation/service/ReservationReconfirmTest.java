package com.Wolgyesangdan.backend.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 수령 재확인 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ReservationService.class})
class ReservationReconfirmTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 12, 0);

	@Autowired
	private ReservationService reservationService;

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
	void 기한_안에_재확인하면_RECONFIRMED가_되고_재확인_일시가_저장된다() {
		Reservation reservation = reservation(ReservationStatus.SCHEDULED, NOW.plusHours(1));
		flushAndClear();

		ReconfirmResponse response = reservationService.reconfirm(applicant.getId(), reservation.getId(), NOW);
		flushAndClear();

		assertThat(response.id()).isEqualTo(reservation.getId());
		assertThat(response.status()).isEqualTo(ReservationStatus.RECONFIRMED);
		assertThat(response.reconfirmedAt()).isEqualTo(NOW);
		Reservation saved = entityManager.find(Reservation.class, reservation.getId());
		assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RECONFIRMED);
		assertThat(saved.getReconfirmedAt()).isEqualTo(NOW);
	}

	@ParameterizedTest
	@EnumSource(value = ReservationStatus.class,
			names = {"SCHEDULED", "HUB_DROP_SCHEDULED", "HUB_RECEIVED", "PICKUP_SCHEDULED"})
	void 진행_중인_예약은_재확인할_수_있다(ReservationStatus status) {
		Reservation reservation = reservation(status, NOW.plusDays(1));
		flushAndClear();

		assertThat(reservationService.reconfirm(applicant.getId(), reservation.getId(), NOW).status())
				.isEqualTo(ReservationStatus.RECONFIRMED);
	}

	@Test
	void 기한_시각_정각까지는_재확인할_수_있다() {
		Reservation reservation = reservation(ReservationStatus.SCHEDULED, NOW);
		flushAndClear();

		assertThat(reservationService.reconfirm(applicant.getId(), reservation.getId(), NOW).status())
				.isEqualTo(ReservationStatus.RECONFIRMED);
	}

	@Test
	void 재확인_기한이_비어_있으면_기한_없이_재확인할_수_있다() {
		Reservation reservation = reservation(ReservationStatus.SCHEDULED, null);
		flushAndClear();

		assertThat(reservationService.reconfirm(applicant.getId(), reservation.getId(), NOW).status())
				.isEqualTo(ReservationStatus.RECONFIRMED);
	}

	@Test
	void 기한이_지났으면_RESERVATION_RECONFIRMATION_EXPIRED() {
		Reservation reservation = reservation(ReservationStatus.SCHEDULED, NOW.minusSeconds(1));
		flushAndClear();

		assertError(applicant, reservation, ReservationErrorCode.RESERVATION_RECONFIRMATION_EXPIRED);
	}

	@Test
	void 이미_재확인했으면_RESERVATION_ALREADY_RECONFIRMED() {
		Reservation reservation = reservation(ReservationStatus.RECONFIRMED, NOW.plusDays(1));
		flushAndClear();

		assertError(applicant, reservation, ReservationErrorCode.RESERVATION_ALREADY_RECONFIRMED);
	}

	@Test
	void 이미_재확인한_예약은_기한이_지나도_ALREADY_RECONFIRMED() {
		Reservation reservation = reservation(ReservationStatus.RECONFIRMED, NOW.minusDays(1));
		flushAndClear();

		assertError(applicant, reservation, ReservationErrorCode.RESERVATION_ALREADY_RECONFIRMED);
	}

	@ParameterizedTest
	@EnumSource(value = ReservationStatus.class, names = {"COMPLETED", "NO_SHOW", "CANCELED"})
	void 끝난_예약은_RESERVATION_NOT_RECONFIRMABLE(ReservationStatus status) {
		Reservation reservation = reservation(status, NOW.plusDays(1));
		flushAndClear();

		assertError(applicant, reservation, ReservationErrorCode.RESERVATION_NOT_RECONFIRMABLE);
	}

	@Test
	void 신청자가_아니면_등록자라도_RESERVATION_NOT_PARTICIPANT() {
		Reservation reservation = reservation(ReservationStatus.SCHEDULED, NOW.plusDays(1));
		flushAndClear();

		assertError(owner, reservation, ReservationErrorCode.RESERVATION_NOT_PARTICIPANT);
	}

	@Test
	void 없는_예약이면_RESERVATION_NOT_FOUND() {
		assertThatThrownBy(() -> reservationService.reconfirm(applicant.getId(), Long.MAX_VALUE, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
	}

	private void assertError(User user, Reservation reservation, ReservationErrorCode errorCode) {
		assertThatThrownBy(() -> reservationService.reconfirm(user.getId(), reservation.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(errorCode);
	}

	private Reservation reservation(ReservationStatus status, LocalDateTime reconfirmationDeadline) {
		Item item = persist(Item.builder()
				.owner(owner)
				.name("1인용 책상")
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(30)
				.availableFrom(LocalDate.now())
				.applicationDeadline(NOW.minusDays(1))
				.status(ItemStatus.ASSIGNED)
				.build());
		Application application = persist(Application.builder()
				.item(item).applicant(applicant).priorityScore(0).status(ApplicationStatus.SELECTED).build());
		return persist(Reservation.builder()
				.application(application)
				.tradeMethod(TradeMethod.DIRECT)
				.scheduledAt(NOW.plusDays(2))
				.status(status)
				.reconfirmationDeadline(reconfirmationDeadline)
				.build());
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
