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
import com.Wolgyesangdan.backend.domain.reservation.dto.ReservationDetailResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
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
 * 실제 MySQL에서 예약 상세 조회 확인. 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ReservationService.class})
class ReservationDetailQueryTest {

	private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 10, 5, 14, 0);
	private static final LocalDateTime RECONFIRMATION_DEADLINE = LocalDateTime.of(2026, 10, 4, 23, 59, 59);

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private EntityManager entityManager;

	private User owner;
	private User applicant;
	private Application application;
	private Reservation reservation;

	@BeforeEach
	void setUp() {
		// 두 사람 다 연락 수단을 한 번씩 바꿔서 phone·openchatLink가 모두 남아 있는 상태
		owner = persist(user("등록자", ContactType.OPENCHAT));
		applicant = persist(user("신청자", ContactType.PHONE));
		application = persist(Application.builder()
				.item(persist(item(owner))).applicant(applicant).priorityScore(0)
				.status(ApplicationStatus.SELECTED).build());
		reservation = persist(Reservation.builder()
				.application(application).tradeMethod(TradeMethod.CAMPAIGN).scheduledAt(SCHEDULED_AT)
				.status(ReservationStatus.PICKUP_SCHEDULED).reconfirmationDeadline(RECONFIRMATION_DEADLINE)
				.build());
		flushAndClear();
	}

	@Test
	void 신청자가_조회하면_등록자_정보를_내려준다() {
		ReservationDetailResponse response = reservationService.getReservation(applicant.getId(),
				application.getId());

		assertThat(response.id()).isEqualTo(reservation.getId());
		assertThat(response.applicationId()).isEqualTo(application.getId());
		assertThat(response.tradeMethod()).isEqualTo(TradeMethod.CAMPAIGN);
		assertThat(response.scheduledAt()).isEqualTo(SCHEDULED_AT);
		assertThat(response.status()).isEqualTo(ReservationStatus.PICKUP_SCHEDULED);
		assertThat(response.reconfirmationDeadline()).isEqualTo(RECONFIRMATION_DEADLINE);
		assertThat(response.reconfirmedAt()).isNull();
		assertThat(response.counterpart().nickname()).isEqualTo("등록자");
		assertThat(response.counterpart().contactType()).isEqualTo(ContactType.OPENCHAT);
		assertThat(response.counterpart().openchatLink()).isEqualTo("https://open.kakao.com/o/abc123");
	}

	@Test
	void 등록자가_조회하면_신청자_정보를_내려준다() {
		ReservationDetailResponse response = reservationService.getReservation(owner.getId(), application.getId());

		assertThat(response.counterpart().nickname()).isEqualTo("신청자");
		assertThat(response.counterpart().contactType()).isEqualTo(ContactType.PHONE);
		assertThat(response.counterpart().phone()).isEqualTo("010-1234-5678");
	}

	@Test
	void 상대가_공개하기로_고른_연락_수단만_내려준다() {
		assertThat(reservationService.getReservation(applicant.getId(), application.getId()).counterpart().phone())
				.isNull();
		assertThat(reservationService.getReservation(owner.getId(), application.getId()).counterpart()
				.openchatLink()).isNull();
	}

	@Test
	void 신청자도_등록자도_아니면_RESERVATION_NOT_PARTICIPANT() {
		User stranger = persist(user("남", ContactType.PHONE));

		assertThatThrownBy(() -> reservationService.getReservation(stranger.getId(), application.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_PARTICIPANT);
	}

	@Test
	void 아직_배정_전이라_예약이_없으면_RESERVATION_NOT_FOUND() {
		User waiter = persist(user("대기자", ContactType.PHONE));
		Application waiting = persist(Application.builder()
				.item(entityManager.getReference(Item.class, application.getItem().getId())).applicant(waiter)
				.priorityScore(0).status(ApplicationStatus.WAITING).build());
		flushAndClear();

		assertThatThrownBy(() -> reservationService.getReservation(waiter.getId(), waiting.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	void 없는_신청이면_RESERVATION_NOT_FOUND() {
		assertThatThrownBy(() -> reservationService.getReservation(applicant.getId(), Long.MAX_VALUE))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	void 쿼리는_1번으로_고정된다() {
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		reservationService.getReservation(applicant.getId(), application.getId());

		// 예약+신청+신청자+물품+등록자
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private static User user(String nickname, ContactType contactType) {
		return User.builder()
				.kakaoId("test-" + UUID.randomUUID())
				.nickname(nickname)
				.contactType(contactType)
				.phone("010-1234-5678")
				.openchatLink("https://open.kakao.com/o/abc123")
				.build();
	}

	private static Item item(User owner) {
		return Item.builder()
				.owner(owner)
				.name("전자레인지")
				.categoryGroup(CategoryGroup.APPLIANCE)
				.category("생활가전")
				.conditionGrade("상태 좋음")
				.transportDifficulty("보통")
				.estimatedCarbonReduction(24)
				.availableFrom(LocalDate.now())
				.availableUntil(LocalDate.now().plusDays(7))
				.applicationDeadline(LocalDateTime.now().plusDays(3))
				.status(ItemStatus.ASSIGNED)
				.build();
	}

}
