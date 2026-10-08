package com.Wolgyesangdan.backend.domain.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.dto.ApplicationCreateResponse;
import com.Wolgyesangdan.backend.domain.application.dto.MyApplicationSummaryResponse;
import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.exception.ApplicationErrorCode;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.service.NeighborhoodLocationService;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;

/**
 * 실제 MySQL에서 물품 신청 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ApplicationService.class, VerificationService.class})
class ApplicationServiceTest {

	// VerificationService가 동네 인증 위치 판정에 쓰는 의존성 — 신청 테스트에서는 쓰지 않는다
	@MockitoBean
	private NeighborhoodLocationService neighborhoodLocationService;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private EntityManager entityManager;

	private User owner;
	private User applicant;

	@BeforeEach
	void setUp() {
		owner = persist(user("등록자"));
		applicant = persistEligibleApplicant("신청자");
	}

	@Test
	void 물품이_없으면_404() {
		assertThatThrownBy(() -> applicationService.apply(applicant.getId(), -1L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_FOUND);
	}

	@Test
	void 관리자가_숨긴_물품이면_404() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		item.hide();

		assertThatThrownBy(() -> applicationService.apply(applicant.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_HIDDEN);
	}

	@Test
	void 물품이_OPEN이_아니면_409() {
		Item item = persist(item(owner, ItemStatus.CLOSED, LocalDateTime.now().plusDays(1), 0));

		assertThatThrownBy(() -> applicationService.apply(applicant.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_ITEM_NOT_OPEN);
	}

	@Test
	void 신청_마감_시각이_지났으면_409() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().minusSeconds(1), 0));

		assertThatThrownBy(() -> applicationService.apply(applicant.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_ITEM_NOT_OPEN);
	}

	@Test
	void 본인_물품이면_403() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));

		assertThatThrownBy(() -> applicationService.apply(owner.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_OWN_ITEM);
	}

	@Test
	void 동네_인증이_없으면_403() {
		User notVerified = persist(user("미인증자"));
		notVerified.updateContact(ContactType.OPENCHAT, null, "https://open.kakao.com/o/abc");
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));

		assertThatThrownBy(() -> applicationService.apply(notVerified.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_NOT_ELIGIBLE);
	}

	@Test
	void 연락_수단이_없으면_403() {
		User noContact = persist(user("연락처없음"));
		persist(neighborhoodVerification(noContact));
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));

		assertThatThrownBy(() -> applicationService.apply(noContact.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_CONTACT_NOT_SET);
	}

	@Test
	void 이미_신청했으면_409() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();

		assertThatThrownBy(() -> applicationService.apply(applicant.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_ALREADY_EXISTS);
	}

	@Test
	void 취소한_신청은_같은_물품에_다시_신청할_수_있다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse first = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		applicationService.cancel(applicant.getId(), first.id());
		entityManager.flush();

		ApplicationCreateResponse again = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();

		assertThat(again.status()).isEqualTo(ApplicationStatus.WAITING);
		assertThat(entityManager.find(Application.class, again.id()).getWaitlistRank()).isEqualTo(1);
		assertThat(again.id()).isNotEqualTo(first.id());
		// 취소한 신청은 지워지고 새 신청 하나만 남는다
		assertThat(entityManager.find(Application.class, first.id())).isNull();
		assertThat(item.getApplicantCount()).isEqualTo(1);
	}

	@Test
	void 다시_신청하면_대기_순서는_맨_뒤가_된다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse first = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		User second = persistEligibleApplicant("두번째신청자");
		ApplicationCreateResponse secondResponse = applicationService.apply(second.getId(), item.getId());
		entityManager.flush();
		applicationService.cancel(applicant.getId(), first.id());
		entityManager.flush();

		ApplicationCreateResponse again = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();

		assertThat(entityManager.find(Application.class, again.id()).getWaitlistRank()).isEqualTo(2);
		assertThat(entityManager.find(Application.class, secondResponse.id()).getWaitlistRank()).isEqualTo(1);
	}

	@Test
	void 노쇼로_빠져_예약이_남은_신청은_다시_신청할_수_없다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		Application dropped = persist(Application.builder()
				.item(item)
				.applicant(applicant)
				.priorityScore(0)
				.status(ApplicationStatus.CANCELED)
				.waitlistRank(1)
				.build());
		persist(Reservation.builder()
				.application(dropped)
				.tradeMethod(TradeMethod.DIRECT)
				.status(ReservationStatus.NO_SHOW)
				.build());
		entityManager.flush();

		assertThatThrownBy(() -> applicationService.apply(applicant.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_ALREADY_EXISTS);
	}

	@Test
	void 신청하면_WAITING으로_저장되고_순번을_받지만_응답으로는_알려주지_않는다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));

		ApplicationCreateResponse response = applicationService.apply(applicant.getId(), item.getId());

		assertThat(response.itemId()).isEqualTo(item.getId());
		assertThat(response.status()).isEqualTo(ApplicationStatus.WAITING);
		// 순번은 매겨 두되(배정에 쓴다), 배정 전에는 신청자에게 알려주지 않는다 (#265)
		assertThat(entityManager.find(Application.class, response.id()).getWaitlistRank()).isEqualTo(1);
		assertThat(response.waitlistRank()).isNull();
	}

	@Test
	void 우선배정_점수가_높으면_먼저_신청한_사람보다_앞선_순번을_받는다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		User first = persistEligibleApplicant("일반신청자");  // 우선배정 없음, 먼저 신청
		User priority = persistEligibleApplicant("신입생신청자");
		persist(priorityVerification(priority, VerificationType.FRESHMAN));

		ApplicationCreateResponse firstResponse = applicationService.apply(first.getId(), item.getId());
		entityManager.flush();
		ApplicationCreateResponse priorityResponse = applicationService.apply(priority.getId(), item.getId());
		entityManager.flush();

		// 우선배정 신청자가 먼저 신청한 사람보다 앞선 순번(1번)을 가져가고, 먼저 신청한 사람은 2번으로 밀린다
		assertThat(entityManager.find(Application.class, priorityResponse.id()).getWaitlistRank()).isEqualTo(1);
		// 순번이 바뀐 것은 누구의 응답으로도 드러나지 않는다 (#265)
		assertThat(priorityResponse.waitlistRank()).isNull();
		Application firstApplication = entityManager.find(Application.class, firstResponse.id());
		assertThat(firstApplication.getWaitlistRank()).isEqualTo(2);
	}

	@Test
	void 다섯번째_신청이면_정원_도달로_CLOSED_전환된다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		for (int i = 0; i < 4; i++) {
			User filler = persistEligibleApplicant("대기자" + i);
			applicationService.apply(filler.getId(), item.getId());
		}
		entityManager.flush();
		assertThat(item.getStatus()).isEqualTo(ItemStatus.OPEN);

		applicationService.apply(applicant.getId(), item.getId());

		assertThat(item.getStatus()).isEqualTo(ItemStatus.CLOSED);
		assertThat(item.getApplicantCount()).isEqualTo(5);
	}

	@Test
	void 존재하지_않는_신청이면_404() {
		assertThatThrownBy(() -> applicationService.cancel(applicant.getId(), -1L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_NOT_FOUND);
	}

	@Test
	void 본인_신청이_아니면_403() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse response = applicationService.apply(applicant.getId(), item.getId());
		User someoneElse = persistEligibleApplicant("다른사람");

		assertThatThrownBy(() -> applicationService.cancel(someoneElse.getId(), response.id()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_NOT_OWNER);
	}

	@Test
	void 이미_배정된_신청이면_409() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		Application selected = persist(Application.builder()
				.item(item)
				.applicant(applicant)
				.priorityScore(0)
				.status(ApplicationStatus.SELECTED)
				.build());

		assertThatThrownBy(() -> applicationService.cancel(applicant.getId(), selected.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_ALREADY_SELECTED);
	}

	@Test
	void 취소하면_CANCELED로_바뀌고_신청자_수가_감소한다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse response = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();

		applicationService.cancel(applicant.getId(), response.id());

		Application canceled = entityManager.find(Application.class, response.id());
		assertThat(canceled.getStatus()).isEqualTo(ApplicationStatus.CANCELED);
		assertThat(canceled.getWaitlistRank()).isNull();
		assertThat(item.getApplicantCount()).isZero();
	}

	@Test
	void 취소하면_남은_대기자_순번이_앞으로_당겨진다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse firstResponse = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		User second = persistEligibleApplicant("두번째신청자");
		ApplicationCreateResponse secondResponse = applicationService.apply(second.getId(), item.getId());
		entityManager.flush();
		assertThat(entityManager.find(Application.class, secondResponse.id()).getWaitlistRank()).isEqualTo(2);

		applicationService.cancel(applicant.getId(), firstResponse.id());

		Application secondApplication = entityManager.find(Application.class, secondResponse.id());
		assertThat(secondApplication.getWaitlistRank()).isEqualTo(1);
	}

	@Test
	void 이미_취소한_신청을_다시_취소하면_409() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse response = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		applicationService.cancel(applicant.getId(), response.id());
		entityManager.flush();

		assertThatThrownBy(() -> applicationService.cancel(applicant.getId(), response.id()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ApplicationErrorCode.APPLICATION_ALREADY_CANCELED);
	}

	@Test
	void 정원_마감으로_CLOSED된_물품도_신청_마감_전이면_취소시_다시_OPEN된다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		User lastFiller = null;
		Long lastApplicationId = null;
		for (int i = 0; i < 5; i++) {
			User filler = persistEligibleApplicant("대기자" + i);
			lastApplicationId = applicationService.apply(filler.getId(), item.getId()).id();
			lastFiller = filler;
		}
		entityManager.flush();
		assertThat(item.getStatus()).isEqualTo(ItemStatus.CLOSED);

		applicationService.cancel(lastFiller.getId(), lastApplicationId);

		assertThat(item.getStatus()).isEqualTo(ItemStatus.OPEN);
		assertThat(item.getApplicantCount()).isEqualTo(4);
	}

	@Test
	void 신청_마감_시각이_지난_뒤에는_취소해도_CLOSED를_유지한다() {
		Item item = persist(item(owner, ItemStatus.CLOSED, LocalDateTime.now().minusSeconds(1), 4));
		Application waiting = persist(Application.builder()
				.item(item)
				.applicant(applicant)
				.priorityScore(0)
				.status(ApplicationStatus.WAITING)
				.build());

		applicationService.cancel(applicant.getId(), waiting.getId());

		assertThat(item.getStatus()).isEqualTo(ItemStatus.CLOSED);
		assertThat(item.getApplicantCount()).isEqualTo(3);
	}

	@Test
	void 내가_신청한_물품_목록을_최근_신청순으로_내려준다() {
		Item older = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		persist(ItemImage.builder().item(older).imageUrl("https://img/older.jpg").displayOrder(0).build());
		ApplicationCreateResponse olderResponse = applicationService.apply(applicant.getId(), older.getId());
		entityManager.flush();

		Item newer = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		persist(ItemImage.builder().item(newer).imageUrl("https://img/newer.jpg").displayOrder(0).build());
		ApplicationCreateResponse newerResponse = applicationService.apply(applicant.getId(), newer.getId());
		entityManager.flush();

		Page<MyApplicationSummaryResponse> result = applicationService.getMyApplications(applicant.getId(), 0, 20);

		assertThat(result.getContent()).hasSize(2);
		MyApplicationSummaryResponse first = result.getContent().get(0);
		assertThat(first.id()).isEqualTo(newerResponse.id());
		assertThat(first.itemId()).isEqualTo(newer.getId());
		assertThat(first.itemName()).isEqualTo(newer.getName());
		assertThat(first.itemThumbnailImageUrl()).isEqualTo("https://img/newer.jpg");
		assertThat(first.status()).isEqualTo(ApplicationStatus.WAITING);
		// 아직 배정 전이라 대기 순번은 내려주지 않는다 (#265)
		assertThat(first.waitlistRank()).isNull();
		assertThat(result.getContent().get(1).id()).isEqualTo(olderResponse.id());
	}

	@Test
	void 대표_사진은_표시_순서가_가장_앞선_사진이다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		persist(ItemImage.builder().item(item).imageUrl("https://img/second.jpg").displayOrder(1).build());
		persist(ItemImage.builder().item(item).imageUrl("https://img/first.jpg").displayOrder(0).build());
		applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();

		Page<MyApplicationSummaryResponse> result = applicationService.getMyApplications(applicant.getId(), 0, 20);

		assertThat(result.getContent().get(0).itemThumbnailImageUrl()).isEqualTo("https://img/first.jpg");
	}

	@Test
	void 물품에_사진이_없으면_대표_사진은_null이다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();

		Page<MyApplicationSummaryResponse> result = applicationService.getMyApplications(applicant.getId(), 0, 20);

		assertThat(result.getContent().get(0).itemThumbnailImageUrl()).isNull();
	}

	@Test
	void 취소하면_waitlistRank가_null로_내려준다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse response = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		applicationService.cancel(applicant.getId(), response.id());
		entityManager.flush();

		Page<MyApplicationSummaryResponse> result = applicationService.getMyApplications(applicant.getId(), 0, 20);

		assertThat(result.getContent().get(0).status()).isEqualTo(ApplicationStatus.CANCELED);
		assertThat(result.getContent().get(0).waitlistRank()).isNull();
	}

	// 배정 확정 후에도 대기 순번을 그대로 보여준다 — "대기 1번이었어요" 화면 (요구사항 APPL-07, 2026-09-26 결정)
	@Test
	void 배정_후에도_waitlistRank가_그대로_내려준다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		ApplicationCreateResponse response = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		Application application = entityManager.find(Application.class, response.id());
		application.select(LocalDateTime.now());
		item.assign();
		entityManager.flush();

		Page<MyApplicationSummaryResponse> result = applicationService.getMyApplications(applicant.getId(), 0, 20);

		assertThat(result.getContent().get(0).status()).isEqualTo(ApplicationStatus.SELECTED);
		assertThat(result.getContent().get(0).waitlistRank()).isEqualTo(1);
	}

	// 배정에서 밀려 대기 중인 신청자도, 배정이 끝난 뒤에는 자기 순번(승계 순서)을 볼 수 있다 (#265)
	@Test
	void 배정이_끝나면_대기_중인_신청자에게도_waitlistRank를_내려준다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		User second = persistEligibleApplicant("두번째신청자");
		ApplicationCreateResponse firstResponse = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		applicationService.apply(second.getId(), item.getId());
		entityManager.flush();

		// 마감 전 — 두 사람 모두 순번을 모른다
		assertThat(applicationService.getMyApplications(applicant.getId(), 0, 20).getContent().get(0).waitlistRank())
				.isNull();
		assertThat(applicationService.getMyApplications(second.getId(), 0, 20).getContent().get(0).waitlistRank())
				.isNull();

		entityManager.find(Application.class, firstResponse.id()).select(LocalDateTime.now());
		item.assign();
		entityManager.flush();

		MyApplicationSummaryResponse waiting = applicationService.getMyApplications(second.getId(), 0, 20).getContent().get(0);
		assertThat(waiting.status()).isEqualTo(ApplicationStatus.WAITING);
		assertThat(waiting.waitlistRank()).isEqualTo(2);
		assertThat(waiting.itemStatus()).isEqualTo(ItemStatus.ASSIGNED);
	}

	// 배정된 사람과 거래가 끝나면, 대기하던 신청자는 물품 상태(COMPLETED)와 자기 순번으로 "배정받지 못함"을 알 수 있다 (#265)
	@Test
	void 거래가_끝난_물품의_대기자에게는_물품_상태_COMPLETED와_순번을_내려준다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));
		User second = persistEligibleApplicant("두번째신청자");
		ApplicationCreateResponse firstResponse = applicationService.apply(applicant.getId(), item.getId());
		entityManager.flush();
		applicationService.apply(second.getId(), item.getId());
		entityManager.flush();
		Application selected = entityManager.find(Application.class, firstResponse.id());
		selected.select(LocalDateTime.now());
		item.assign();
		selected.complete();
		item.complete();
		entityManager.flush();

		MyApplicationSummaryResponse missed = applicationService.getMyApplications(second.getId(), 0, 20).getContent().get(0);
		assertThat(missed.status()).isEqualTo(ApplicationStatus.WAITING);
		assertThat(missed.itemStatus()).isEqualTo(ItemStatus.COMPLETED);
		assertThat(missed.waitlistRank()).isEqualTo(2);
		MyApplicationSummaryResponse received = applicationService.getMyApplications(applicant.getId(), 0, 20).getContent().get(0);
		assertThat(received.status()).isEqualTo(ApplicationStatus.COMPLETED);
		assertThat(received.itemStatus()).isEqualTo(ItemStatus.COMPLETED);
	}

	@Test
	void 신청한_적이_없으면_빈_목록() {
		Page<MyApplicationSummaryResponse> result = applicationService.getMyApplications(applicant.getId(), 0, 20);

		assertThat(result.getContent()).isEmpty();
	}

	private User persistEligibleApplicant(String nickname) {
		User user = persist(user(nickname));
		user.updateContact(ContactType.OPENCHAT, null, "https://open.kakao.com/o/abc");
		persist(neighborhoodVerification(user));
		return user;
	}

	private User user(String nickname) {
		return User.builder().kakaoId("test-" + UUID.randomUUID()).nickname(nickname).build();
	}

	private Item item(User owner, ItemStatus status, LocalDateTime applicationDeadline, int applicantCount) {
		return Item.builder()
				.owner(owner)
				.name("냄비")
				.categoryGroup(CategoryGroup.KITCHEN)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(10)
				.applicationDeadline(applicationDeadline)
				.status(status)
				.applicantCount(applicantCount)
				.build();
	}

	private PriorityVerification neighborhoodVerification(User user) {
		return PriorityVerification.builder()
				.user(user)
				.verificationType(VerificationType.NEIGHBORHOOD)
				.status(VerificationStatus.APPROVED)
				.submittedAt(LocalDateTime.now())
				.build();
	}

	private PriorityVerification priorityVerification(User user, VerificationType type) {
		return PriorityVerification.builder()
				.user(user)
				.verificationType(type)
				.status(VerificationStatus.APPROVED)
				.submittedAt(LocalDateTime.now())
				.build();
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

}
