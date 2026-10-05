package com.Wolgyesangdan.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class VerificationServiceTest {

	private static final Long USER_ID = 1L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 15, 0);
	private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 9, 10, 9, 0);

	private final PriorityVerificationRepository priorityVerificationRepository = Mockito
			.mock(PriorityVerificationRepository.class);
	private final VerificationService verificationService = new VerificationService(priorityVerificationRepository);

	@Test
	void 신청한_적_없으면_빈_목록() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of());

		assertThat(verificationService.getMyVerifications(USER_ID, NOW)).isEmpty();
	}

	@Test
	void 신청한_적_있는_유형만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(1L, VerificationType.FRESHMAN, VerificationStatus.PENDING, SUBMITTED_AT)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses).extracting(MyVerificationResponse::verificationType)
				.containsExactly(VerificationType.FRESHMAN);
	}

	@Test
	void 유형마다_가장_최근_제출_건_기준으로_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(2L, VerificationType.FRESHMAN, VerificationStatus.PENDING, SUBMITTED_AT.plusDays(5)),  // 재신청
				verification(1L, VerificationType.FRESHMAN, VerificationStatus.REJECTED, SUBMITTED_AT),
				verification(3L, VerificationType.RESIDENT, VerificationStatus.APPROVED, SUBMITTED_AT.plusDays(1))));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).verificationType()).isEqualTo(VerificationType.RESIDENT);
		assertThat(responses.get(0).status()).isEqualTo(VerificationStatus.APPROVED);
		assertThat(responses.get(1).verificationType()).isEqualTo(VerificationType.FRESHMAN);
		assertThat(responses.get(1).status()).isEqualTo(VerificationStatus.PENDING);
		assertThat(responses.get(1).submittedAt()).isEqualTo(SUBMITTED_AT.plusDays(5));
	}

	@Test
	void 제출_시각이_같으면_나중에_만들어진_건이_최근_건() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(1L, VerificationType.LOW_INCOME, VerificationStatus.REJECTED, SUBMITTED_AT),
				verification(2L, VerificationType.LOW_INCOME, VerificationStatus.PENDING, SUBMITTED_AT)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).status()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void 유형_순서는_RESIDENT_FRESHMAN_LOW_INCOME() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(1L, VerificationType.LOW_INCOME, VerificationStatus.PENDING, SUBMITTED_AT),
				verification(2L, VerificationType.FRESHMAN, VerificationStatus.PENDING, SUBMITTED_AT),
				verification(3L, VerificationType.RESIDENT, VerificationStatus.PENDING, SUBMITTED_AT)));

		assertThat(verificationService.getMyVerifications(USER_ID, NOW))
				.extracting(MyVerificationResponse::verificationType)
				.containsExactly(VerificationType.RESIDENT, VerificationType.FRESHMAN, VerificationType.LOW_INCOME);
	}

	@Test
	void 반려_사유는_REJECTED일_때만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				reviewed(1L, VerificationType.FRESHMAN, VerificationStatus.REJECTED),
				reviewed(2L, VerificationType.RESIDENT, VerificationStatus.APPROVED)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).rejectionReason()).isNull();                 // RESIDENT, APPROVED
		assertThat(responses.get(1).rejectionReason()).isEqualTo("반려 사유");   // FRESHMAN, REJECTED
	}

	@Test
	void 만료_일시는_APPROVED일_때만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				reviewed(1L, VerificationType.FRESHMAN, VerificationStatus.REJECTED),
				reviewed(2L, VerificationType.RESIDENT, VerificationStatus.APPROVED),
				reviewed(3L, VerificationType.LOW_INCOME, VerificationStatus.EXPIRED)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).expiresAt()).isEqualTo(SUBMITTED_AT.plusMonths(6));  // RESIDENT, APPROVED
		assertThat(responses.get(1).expiresAt()).isNull();                               // FRESHMAN, REJECTED
		assertThat(responses.get(2).expiresAt()).isNull();                               // LOW_INCOME, EXPIRED
	}

	@Test
	void 만료일이_지난_승인은_EXPIRED로_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, NOW.minusSeconds(1)),  // 만료됨
				approved(2L, VerificationType.RESIDENT, NOW.plusSeconds(1)),   // 아직 유효
				approved(3L, VerificationType.LOW_INCOME, null)));             // 만료일 없음

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).status()).isEqualTo(VerificationStatus.APPROVED);   // RESIDENT
		assertThat(responses.get(0).expiresAt()).isEqualTo(NOW.plusSeconds(1));
		assertThat(responses.get(1).status()).isEqualTo(VerificationStatus.EXPIRED);    // FRESHMAN
		assertThat(responses.get(1).expiresAt()).isNull();
		assertThat(responses.get(2).status()).isEqualTo(VerificationStatus.APPROVED);   // LOW_INCOME
		assertThat(responses.get(2).expiresAt()).isNull();
	}

	private static PriorityVerification approved(Long id, VerificationType type, LocalDateTime expiresAt) {
		PriorityVerification verification = PriorityVerification.builder()
				.verificationType(type)
				.status(VerificationStatus.APPROVED)
				.submittedAt(SUBMITTED_AT)
				.reviewedAt(SUBMITTED_AT.plusDays(1))
				.expiresAt(expiresAt)
				.build();
		ReflectionTestUtils.setField(verification, "id", id);
		return verification;
	}

	private static PriorityVerification verification(Long id, VerificationType type, VerificationStatus status,
			LocalDateTime submittedAt) {
		PriorityVerification verification = PriorityVerification.builder()
				.verificationType(type)
				.status(status)
				.submittedAt(submittedAt)
				.build();
		ReflectionTestUtils.setField(verification, "id", id);
		return verification;
	}

	// 운영진이 심사하면서 사유·만료 일시를 상태와 무관하게 채워둔 경우
	private static PriorityVerification reviewed(Long id, VerificationType type, VerificationStatus status) {
		PriorityVerification verification = PriorityVerification.builder()
				.verificationType(type)
				.status(status)
				.submittedAt(SUBMITTED_AT)
				.reviewedAt(SUBMITTED_AT.plusDays(1))
				.rejectionReason("반려 사유")
				.expiresAt(SUBMITTED_AT.plusMonths(6))
				.build();
		ReflectionTestUtils.setField(verification, "id", id);
		return verification;
	}

}
