package com.Wolgyesangdan.backend.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.dto.DeliveryTodo;
import com.Wolgyesangdan.backend.domain.reservation.dto.MyTodoResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmTodo;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 "내가 지금 해야 할 일"(#187) 확인. 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ReservationService.class})
class ReservationTodoQueryTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 12, 0);

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
	void 배정됐고_기한이_남은_재확인_전_예약은_신청자_할_일에_나온다() {
		Reservation reservation = reservation(owner, applicant, "전자레인지", ReservationStatus.SCHEDULED, NOW.plusHours(5));
		flushAndClear();

		MyTodoResponse todo = reservationService.getMyTodo(applicant.getId(), NOW);

		assertThat(todo.reconfirms()).singleElement().satisfies(r -> {
			assertThat(r.reservationId()).isEqualTo(reservation.getId());
			assertThat(r.applicationId()).isEqualTo(reservation.getApplication().getId());
			assertThat(r.itemId()).isEqualTo(reservation.getApplication().getItem().getId());
			assertThat(r.itemName()).isEqualTo("전자레인지");
			assertThat(r.reconfirmationDeadline()).isEqualTo(NOW.plusHours(5));
		});
		assertThat(todo.deliveries()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = ReservationStatus.class, names = {"HUB_DROP_SCHEDULED", "HUB_RECEIVED", "PICKUP_SCHEDULED"})
	void 거점_거래_진행_중인_예약도_재확인_할_일이다(ReservationStatus status) {
		reservation(owner, applicant, "선풍기", status, NOW.plusHours(5));
		flushAndClear();

		assertThat(reservationService.getMyTodo(applicant.getId(), NOW).reconfirms()).hasSize(1);
	}

	@Test
	void 이미_재확인했거나_기한이_지났으면_재확인_할_일이_아니다() {
		Reservation reconfirmed = reservation(owner, applicant, "책상", ReservationStatus.SCHEDULED, NOW.plusHours(5));
		reconfirmed.reconfirm(NOW.minusHours(1));
		reservation(owner, applicant, "의자", ReservationStatus.SCHEDULED, NOW.minusMinutes(1));
		flushAndClear();

		assertThat(reservationService.getMyTodo(applicant.getId(), NOW).reconfirms()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = ReservationStatus.class, names = {"COMPLETED", "NO_SHOW", "CANCELED"})
	void 끝난_예약은_신청자에게도_등록자에게도_할_일이_아니다(ReservationStatus status) {
		reservation(owner, applicant, "냄비", status, NOW.plusHours(5));
		flushAndClear();

		assertThat(reservationService.getMyTodo(applicant.getId(), NOW).reconfirms()).isEmpty();
		assertThat(reservationService.getMyTodo(owner.getId(), NOW).deliveries()).isEmpty();
	}

	@Test
	void 재확인_할_일은_기한_빠른_순이고_기한이_없는_예약은_맨_뒤다() {
		reservation(owner, applicant, "늦은 기한", ReservationStatus.SCHEDULED, NOW.plusHours(10));
		reservation(owner, applicant, "기한 없음", ReservationStatus.SCHEDULED, null);
		reservation(owner, applicant, "빠른 기한", ReservationStatus.SCHEDULED, NOW.plusHours(2));
		flushAndClear();

		assertThat(reservationService.getMyTodo(applicant.getId(), NOW).reconfirms())
				.extracting(ReconfirmTodo::itemName)
				.containsExactly("빠른 기한", "늦은 기한", "기한 없음");
	}

	@Test
	void 등록자에게는_끝나지_않은_예약이_전달_할_일로_나오고_재확인_여부를_알려준다() {
		reservation(owner, applicant, "재확인 전", ReservationStatus.SCHEDULED, NOW.plusHours(5));
		Reservation reconfirmed = reservation(owner, applicant, "재확인 함", ReservationStatus.SCHEDULED, NOW.plusHours(5));
		reconfirmed.reconfirm(NOW.minusHours(1));
		flushAndClear();

		MyTodoResponse todo = reservationService.getMyTodo(owner.getId(), NOW);

		assertThat(todo.deliveries()).extracting(DeliveryTodo::itemName, DeliveryTodo::reconfirmed, DeliveryTodo::tradeMethod)
				.containsExactly(
						tuple("재확인 전", false, TradeMethod.DIRECT),
						tuple("재확인 함", true, TradeMethod.DIRECT));
		// 등록자는 신청자가 아니므로 재확인 할 일은 없다
		assertThat(todo.reconfirms()).isEmpty();
	}

	@Test
	void 남의_신청이나_남의_물품은_나오지_않는다() {
		User other = persist(user("다른 사람"));
		reservation(other, other, "남의 거래", ReservationStatus.SCHEDULED, NOW.plusHours(5));
		flushAndClear();

		MyTodoResponse todo = reservationService.getMyTodo(applicant.getId(), NOW);

		assertThat(todo.reconfirms()).isEmpty();
		assertThat(todo.deliveries()).isEmpty();
	}

	@Test
	void 쿼리는_2번으로_고정된다() {
		for (int i = 0; i < 3; i++) {
			reservation(owner, applicant, "물품" + i, ReservationStatus.SCHEDULED, NOW.plusHours(i + 1));
		}
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		reservationService.getMyTodo(applicant.getId(), NOW);

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
	}

	private Reservation reservation(User itemOwner, User itemApplicant, String itemName, ReservationStatus status,
			LocalDateTime reconfirmationDeadline) {
		Item item = persist(Item.builder()
				.owner(itemOwner)
				.name(itemName)
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(30)
				.availableFrom(LocalDate.now())
				.applicationDeadline(NOW.minusDays(1))
				.status(ItemStatus.ASSIGNED)
				.build());
		Application application = persist(Application.builder()
				.item(item).applicant(itemApplicant).priorityScore(0).status(ApplicationStatus.SELECTED).build());
		return persist(Reservation.builder()
				.application(application)
				.tradeMethod(TradeMethod.DIRECT)
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
