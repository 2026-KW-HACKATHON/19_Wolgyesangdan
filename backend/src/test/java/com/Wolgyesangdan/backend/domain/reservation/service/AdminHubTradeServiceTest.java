package com.Wolgyesangdan.backend.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.service.SuccessionService;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.reservation.dto.AdminHubTradeResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 실제 MySQL에서 관리자 거점 거래 처리 확인. 각 테스트는 거래 테이블을 비운 상태에서 시작하고, 끝나면 롤백된다.
 *
 * SuccessionServiceTest와 같은 설정을 써서 테스트 컨텍스트(와 DB 연결 풀)를 새로 만들지 않고 같이 쓴다 —
 * 그래서 AdminHubTradeService는 빈으로 받지 않고 직접 만든다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, SuccessionService.class})
class AdminHubTradeServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 10, 12, 0);

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private SuccessionService successionService;

	@Autowired
	private EntityManager entityManager;

	private AdminHubTradeService adminHubTradeService;
	private User owner;
	private User applicant;

	@BeforeEach
	void setUp() {
		adminHubTradeService = new AdminHubTradeService(reservationRepository, itemRepository, successionService);
		for (String entity : new String[] {"Reservation", "Application", "ItemImage", "ItemTradeMethod", "Item"}) {
			entityManager.createQuery("delete from " + entity).executeUpdate();
		}
		owner = persist(user("등록자"));
		applicant = persist(user("신청자"));
	}

	// ── 목록 ──

	@Test
	void 거점_거래_예약만_최근_배정순으로_내려준다() {
		Reservation older = hubTrade(item("책상"), applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		Reservation newer = hubTrade(item("의자"), applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		reservation(item("직거래 물품"), applicant, TradeMethod.DIRECT, ReservationStatus.SCHEDULED);
		flushAndClear();

		Page<AdminHubTradeResponse> page = adminHubTradeService.getHubTrades(null, PageRequest.of(0, 20, Sort.by("status")));

		assertThat(page.getContent()).extracting(AdminHubTradeResponse::id).containsExactly(newer.getId(), older.getId());
		AdminHubTradeResponse first = page.getContent().getFirst();
		assertThat(first.itemName()).isEqualTo("의자");
		assertThat(first.ownerNickname()).isEqualTo("등록자");
		assertThat(first.applicantNickname()).isEqualTo("신청자");
		assertThat(first.status()).isEqualTo(ReservationStatus.HUB_DROP_SCHEDULED);
		assertThat(first.atHub()).isFalse();
		assertThat(first.hubReceivedAt()).isNull();
		assertThat(first.assignedAt()).isNotNull();
	}

	@Test
	void done으로_진행_중인_거래와_끝난_거래를_거른다() {
		Reservation waiting = hubTrade(item("입고 전"), applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		Reservation reconfirmed = hubTrade(item("재확인"), applicant, ReservationStatus.RECONFIRMED);
		Reservation completed = hubTrade(item("완료"), applicant, ReservationStatus.COMPLETED);
		Reservation noShow = hubTrade(item("미수령"), applicant, ReservationStatus.NO_SHOW);
		flushAndClear();

		assertThat(adminHubTradeService.getHubTrades(false, PageRequest.of(0, 20)).getContent())
				.extracting(AdminHubTradeResponse::id).containsExactly(reconfirmed.getId(), waiting.getId());
		assertThat(adminHubTradeService.getHubTrades(true, PageRequest.of(0, 20)).getContent())
				.extracting(AdminHubTradeResponse::id).containsExactly(noShow.getId(), completed.getId());
		assertThat(adminHubTradeService.getHubTrades(null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(4);
		assertThat(adminHubTradeService.getHubTrades(null, PageRequest.of(0, 1000)).getSize()).isEqualTo(100);
	}

	// ── 입고 ──

	@Test
	void 입고_처리하면_거점_보관_중이_되고_입고_시각이_기록된다() {
		Reservation reservation = hubTrade(item("책상"), applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		flushAndClear();

		AdminHubTradeResponse response = adminHubTradeService.receive(reservation.getId(), NOW);
		flushAndClear();

		assertThat(response.status()).isEqualTo(ReservationStatus.HUB_RECEIVED);
		assertThat(response.atHub()).isTrue();
		Reservation saved = entityManager.find(Reservation.class, reservation.getId());
		assertThat(saved.getStatus()).isEqualTo(ReservationStatus.HUB_RECEIVED);
		assertThat(saved.getHubReceivedAt()).isEqualTo(NOW);
	}

	@Test
	void 신청자가_먼저_재확인한_예약은_입고해도_수령_확정_상태를_지킨다() {
		Reservation reservation = hubTrade(item("책상"), applicant, ReservationStatus.RECONFIRMED);
		flushAndClear();

		AdminHubTradeResponse response = adminHubTradeService.receive(reservation.getId(), NOW);

		assertThat(response.status()).isEqualTo(ReservationStatus.RECONFIRMED);
		assertThat(response.atHub()).isTrue();
		assertThat(response.hubReceivedAt()).isEqualTo(NOW);
	}

	@Test
	void 이미_입고된_물품을_다시_입고하면_409_RESERVATION_ALREADY_AT_HUB() {
		Reservation reservation = hubTrade(item("책상"), applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		adminHubTradeService.receive(reservation.getId(), NOW);

		assertThatThrownBy(() -> adminHubTradeService.receive(reservation.getId(), NOW.plusHours(1)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_ALREADY_AT_HUB);
	}

	// ── 수령 완료 ──

	@Test
	void 수령_완료하면_예약_신청_물품이_모두_거래_완료가_된다() {
		Item item = item("책상");
		Reservation reservation = hubTrade(item, applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		adminHubTradeService.receive(reservation.getId(), NOW);
		flushAndClear();

		// 신청자가 앱에서 재확인하지 않았어도 완료할 수 있다
		AdminHubTradeResponse response = adminHubTradeService.complete(reservation.getId(), NOW.plusHours(3));
		flushAndClear();

		assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
		assertThat(response.completedAt()).isEqualTo(NOW.plusHours(3));
		Reservation saved = entityManager.find(Reservation.class, reservation.getId());
		assertThat(saved.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
		assertThat(saved.getApplication().getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
		assertThat(entityManager.find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.COMPLETED);
	}

	@Test
	void 입고_전에는_수령_완료와_미수령_처리를_할_수_없다() {
		Reservation reservation = hubTrade(item("책상"), applicant, ReservationStatus.RECONFIRMED);

		assertThatThrownBy(() -> adminHubTradeService.complete(reservation.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_AT_HUB);
		assertThatThrownBy(() -> adminHubTradeService.markNoShow(reservation.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_AT_HUB);
	}

	// ── 미수령 ──

	@Test
	void 미수령_처리하면_다음_대기자에게_넘어가고_새_예약은_입고된_상태로_수령_예정이다() {
		Item item = item("책상");
		Reservation reservation = hubTrade(item, applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		User waiter = persist(user("대기자"));
		Application waiting = persist(Application.builder()
				.item(item).applicant(waiter).priorityScore(0).status(ApplicationStatus.WAITING).waitlistRank(2).build());
		adminHubTradeService.receive(reservation.getId(), NOW);
		flushAndClear();

		AdminHubTradeResponse response = adminHubTradeService.markNoShow(reservation.getId(), NOW.plusDays(1));
		flushAndClear();

		assertThat(response.status()).isEqualTo(ReservationStatus.NO_SHOW);
		Reservation dropped = entityManager.find(Reservation.class, reservation.getId());
		assertThat(dropped.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
		assertThat(dropped.getApplication().getStatus()).isEqualTo(ApplicationStatus.CANCELED);
		// 다음 대기자가 배정되고, 물품은 거점에 그대로 있으므로 입고 시각을 이어받는다
		assertThat(entityManager.find(Application.class, waiting.getId()).getStatus()).isEqualTo(ApplicationStatus.SELECTED);
		Reservation next = reservationRepository.findWithParticipantsByApplicationId(waiting.getId()).orElseThrow();
		assertThat(next.getStatus()).isEqualTo(ReservationStatus.PICKUP_SCHEDULED);
		assertThat(next.getTradeMethod()).isEqualTo(TradeMethod.CAMPAIGN);
		assertThat(next.getHubReceivedAt()).isEqualTo(NOW);
		assertThat(next.getReconfirmationDeadline()).isEqualTo(NOW.plusDays(2));
		assertThat(entityManager.find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.ASSIGNED);
		// 넘겨받은 예약은 바로 수령 완료할 수 있다
		assertThat(adminHubTradeService.complete(next.getId(), NOW.plusDays(2)).status())
				.isEqualTo(ReservationStatus.COMPLETED);
	}

	@Test
	void 미수령_처리했는데_대기자가_없으면_물품을_종료한다() {
		Item item = item("책상");
		Reservation reservation = hubTrade(item, applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		adminHubTradeService.receive(reservation.getId(), NOW);
		flushAndClear();

		adminHubTradeService.markNoShow(reservation.getId(), NOW.plusDays(1));
		flushAndClear();

		assertThat(entityManager.find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.CANCELED);
		assertThat(entityManager.find(Reservation.class, reservation.getId()).getApplication().getStatus())
				.isEqualTo(ApplicationStatus.CANCELED);
	}

	// ── 공통 오류 ──

	@Test
	void 직거래_예약은_처리할_수_없다() {
		Reservation direct = reservation(item("직거래 물품"), applicant, TradeMethod.DIRECT, ReservationStatus.RECONFIRMED);

		assertThatThrownBy(() -> adminHubTradeService.receive(direct.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_HUB_TRADE);
		assertThatThrownBy(() -> adminHubTradeService.complete(direct.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_HUB_TRADE);
	}

	@Test
	void 이미_끝난_예약은_처리할_수_없다() {
		Reservation reservation = hubTrade(item("책상"), applicant, ReservationStatus.HUB_DROP_SCHEDULED);
		adminHubTradeService.receive(reservation.getId(), NOW);
		adminHubTradeService.complete(reservation.getId(), NOW);

		assertThatThrownBy(() -> adminHubTradeService.complete(reservation.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_ALREADY_CLOSED);
		assertThatThrownBy(() -> adminHubTradeService.markNoShow(reservation.getId(), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_ALREADY_CLOSED);
	}

	@Test
	void 없는_예약이면_404_RESERVATION_NOT_FOUND() {
		assertThatThrownBy(() -> adminHubTradeService.receive(Long.MAX_VALUE, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
	}

	private Reservation hubTrade(Item item, User user, ReservationStatus status) {
		return reservation(item, user, TradeMethod.CAMPAIGN, status);
	}

	private Reservation reservation(Item item, User user, TradeMethod tradeMethod, ReservationStatus status) {
		Application application = persist(Application.builder()
				.item(item).applicant(user).priorityScore(0).status(ApplicationStatus.SELECTED).waitlistRank(1).build());
		return persist(Reservation.builder()
				.application(application).tradeMethod(tradeMethod).status(status)
				.reconfirmationDeadline(NOW.plusDays(1)).build());
	}

	private Item item(String name) {
		return persist(Item.builder()
				.owner(owner)
				.name(name)
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(30)
				.availableFrom(LocalDate.now())
				.applicationDeadline(NOW.minusDays(2))
				.status(ItemStatus.ASSIGNED)
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
