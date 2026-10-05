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
import com.Wolgyesangdan.backend.domain.reservation.dto.CompleteResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
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
 * 실제 MySQL에서 직거래 전달 완료 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ReservationService.class})
class ReservationCompleteTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 12, 15, 0);

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationRepository reservationRepository;

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
	void 재확인한_직거래를_완료하면_예약_신청_물품이_모두_거래_완료가_된다() {
		Reservation reservation = reservation(TradeMethod.DIRECT, ReservationStatus.RECONFIRMED);
		flushAndClear();

		CompleteResponse response = reservationService.complete(owner.getId(), reservation.getId(), NOW);
		flushAndClear();

		assertThat(response.id()).isEqualTo(reservation.getId());
		assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
		assertThat(response.completedAt()).isEqualTo(NOW);
		Reservation saved = entityManager.find(Reservation.class, reservation.getId());
		assertThat(saved.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
		assertThat(saved.getCompletedAt()).isEqualTo(NOW);
		assertThat(saved.getApplication().getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
		assertThat(saved.getApplication().getItem().getStatus()).isEqualTo(ItemStatus.COMPLETED);
	}

	@Test
	void 완료하면_등록자의_전달_완료_횟수에_들어간다() {
		Reservation reservation = reservation(TradeMethod.DIRECT, ReservationStatus.RECONFIRMED);
		flushAndClear();
		assertThat(reservationRepository.countCompletedByItemOwnerId(owner.getId())).isZero();

		reservationService.complete(owner.getId(), reservation.getId(), NOW);
		flushAndClear();

		assertThat(reservationRepository.countCompletedByItemOwnerId(owner.getId())).isEqualTo(1);
	}

	@Test
	void 재확인_전이면_RESERVATION_NOT_RECONFIRMED() {
		Reservation reservation = reservation(TradeMethod.DIRECT, ReservationStatus.SCHEDULED);
		flushAndClear();

		assertError(owner, reservation, ReservationErrorCode.RESERVATION_NOT_RECONFIRMED);
	}

	@Test
	void 거점_거래면_RESERVATION_NOT_DIRECT() {
		Reservation reservation = reservation(TradeMethod.CAMPAIGN, ReservationStatus.RECONFIRMED);
		flushAndClear();

		assertError(owner, reservation, ReservationErrorCode.RESERVATION_NOT_DIRECT);
	}

	@Test
	void 이미_완료했으면_RESERVATION_ALREADY_COMPLETED() {
		Reservation reservation = reservation(TradeMethod.DIRECT, ReservationStatus.COMPLETED);
		flushAndClear();

		assertError(owner, reservation, ReservationErrorCode.RESERVATION_ALREADY_COMPLETED);
	}

	@ParameterizedTest
	@EnumSource(value = ReservationStatus.class, names = {"NO_SHOW", "CANCELED"})
	void 노쇼_취소된_예약은_RESERVATION_NOT_COMPLETABLE(ReservationStatus status) {
		Reservation reservation = reservation(TradeMethod.DIRECT, status);
		flushAndClear();

		assertError(owner, reservation, ReservationErrorCode.RESERVATION_NOT_COMPLETABLE);
	}

	@Test
	void 등록자가_아니면_신청자라도_RESERVATION_NOT_ITEM_OWNER() {
		Reservation reservation = reservation(TradeMethod.DIRECT, ReservationStatus.RECONFIRMED);
		flushAndClear();

		assertError(applicant, reservation, ReservationErrorCode.RESERVATION_NOT_ITEM_OWNER);
	}

	@Test
	void 없는_예약이면_RESERVATION_NOT_FOUND() {
		assertThatThrownBy(() -> reservationService.complete(owner.getId(), Long.MAX_VALUE, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
	}

	private void assertError(User user, Reservation reservation, ReservationErrorCode errorCode) {
		assertThatThrownBy(() -> reservationService.complete(user.getId(), reservation.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(errorCode);
	}

	private Reservation reservation(TradeMethod tradeMethod, ReservationStatus status) {
		Item item = persist(Item.builder()
				.owner(owner)
				.name("1인용 책상")
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(30)
				.availableFrom(LocalDate.now())
				.applicationDeadline(NOW.minusDays(2))
				.status(ItemStatus.ASSIGNED)
				.build());
		Application application = persist(Application.builder()
				.item(item).applicant(applicant).priorityScore(0).status(ApplicationStatus.SELECTED).build());
		return persist(Reservation.builder()
				.application(application)
				.tradeMethod(tradeMethod)
				.status(status)
				.reconfirmationDeadline(NOW.minusDays(1))
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
