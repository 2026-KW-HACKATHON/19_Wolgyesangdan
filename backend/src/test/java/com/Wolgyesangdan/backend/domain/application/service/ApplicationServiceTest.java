package com.Wolgyesangdan.backend.domain.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.dto.ApplicationCreateResponse;
import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.exception.ApplicationErrorCode;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
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

/**
 * 실제 MySQL에서 물품 신청 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ApplicationService.class, VerificationService.class})
class ApplicationServiceTest {

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
	void 신청하면_WAITING으로_저장되고_순번을_받는다() {
		Item item = persist(item(owner, ItemStatus.OPEN, LocalDateTime.now().plusDays(1), 0));

		ApplicationCreateResponse response = applicationService.apply(applicant.getId(), item.getId());

		assertThat(response.itemId()).isEqualTo(item.getId());
		assertThat(response.status()).isEqualTo(ApplicationStatus.WAITING);
		assertThat(response.waitlistRank()).isEqualTo(1);
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
		assertThat(priorityResponse.waitlistRank()).isEqualTo(1);
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
