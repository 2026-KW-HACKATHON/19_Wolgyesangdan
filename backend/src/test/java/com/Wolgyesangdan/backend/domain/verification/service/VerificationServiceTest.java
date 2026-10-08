package com.Wolgyesangdan.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class VerificationServiceTest {

	private static final Long USER_ID = 1L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 15, 0);
	private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 9, 10, 9, 0);

	private static final VerificationCreateRequest FRESHMAN_REQUEST =
			new VerificationCreateRequest(VerificationType.FRESHMAN, DocumentType.ADMISSION_LETTER,
					"verifications/2026/10/08/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.jpg", " 김하늘 ");
	private static final VerificationCreateRequest LOW_INCOME_REQUEST =
			new VerificationCreateRequest(VerificationType.LOW_INCOME, DocumentType.RECIPIENT_CERTIFICATE,
					"verifications/2026/10/08/1c7f4d3f-6e2b-4d9f-8a8b-2345678901bc.pdf", "이도윤");

	private final PriorityVerificationRepository priorityVerificationRepository = Mockito
			.mock(PriorityVerificationRepository.class);
	private final UserRepository userRepository = Mockito.mock(UserRepository.class);
	private final VerificationService verificationService = new VerificationService(priorityVerificationRepository,
			userRepository);

	@Test
	void 신입생_인증을_신청하면_서류_파일과_실명을_함께_PENDING으로_저장한다() {
		givenUserExists();

		VerificationCreateResponse response = verificationService.createVerification(USER_ID, FRESHMAN_REQUEST, NOW);

		PriorityVerification saved = savedVerification();
		assertThat(saved.getUser().getId()).isEqualTo(USER_ID);
		assertThat(saved.getVerificationType()).isEqualTo(VerificationType.FRESHMAN);
		assertThat(saved.getDocumentType()).isEqualTo(DocumentType.ADMISSION_LETTER);
		assertThat(saved.getFileKey()).isEqualTo("verifications/2026/10/08/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.jpg");
		assertThat(saved.getApplicantName()).isEqualTo("김하늘");
		assertThat(saved.getStatus()).isEqualTo(VerificationStatus.PENDING);
		assertThat(saved.getSubmittedAt()).isEqualTo(NOW);
		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.verificationType()).isEqualTo(VerificationType.FRESHMAN);
		assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);
		assertThat(response.submittedAt()).isEqualTo(NOW);
	}

	@Test
	void 기초수급자_인증을_신청하면_수급자_증명서로_저장한다() {
		givenUserExists();

		verificationService.createVerification(USER_ID, LOW_INCOME_REQUEST, NOW);

		PriorityVerification saved = savedVerification();
		assertThat(saved.getVerificationType()).isEqualTo(VerificationType.LOW_INCOME);
		assertThat(saved.getDocumentType()).isEqualTo(DocumentType.RECIPIENT_CERTIFICATE);
		assertThat(saved.getStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void 같은_유형에_심사_중인_신청이_있으면_거절한다() {
		givenUserExists();
		given(priorityVerificationRepository.existsByUserIdAndVerificationTypeAndStatus(USER_ID,
				VerificationType.LOW_INCOME, VerificationStatus.PENDING)).willReturn(true);

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID, LOW_INCOME_REQUEST, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_PENDING);
		then(priorityVerificationRepository).should(never()).save(any());
	}

	@Test
	void 같은_유형에_유효한_승인이_있으면_거절한다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, NOW.plusSeconds(1))));

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID, FRESHMAN_REQUEST, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
		then(priorityVerificationRepository).should(never()).save(any());
	}

	@Test
	void 만료일이_없는_승인이_있어도_거절한다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.LOW_INCOME, null)));

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID, LOW_INCOME_REQUEST, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
	}

	@Test
	void 승인이_만료됐으면_다시_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, NOW.minusSeconds(1))));

		verificationService.createVerification(USER_ID, FRESHMAN_REQUEST, NOW);

		assertThat(savedVerification().getStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void 승인_뒤에_낸_재신청이_반려됐으면_다시_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, NOW.minusDays(1)),  // 만료된 예전 승인
				verification(2L, VerificationType.FRESHMAN, VerificationStatus.REJECTED, SUBMITTED_AT.plusDays(5))));

		verificationService.createVerification(USER_ID, FRESHMAN_REQUEST, NOW);

		assertThat(savedVerification().getStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void 다른_유형이_승인돼_있어도_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.NEIGHBORHOOD, null),
				approved(2L, VerificationType.FRESHMAN, NOW.plusDays(30))));

		verificationService.createVerification(USER_ID, LOW_INCOME_REQUEST, NOW);

		assertThat(savedVerification().getVerificationType()).isEqualTo(VerificationType.LOW_INCOME);
	}

	@Test
	void 다른_유형이_심사_중이어도_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.existsByUserIdAndVerificationTypeAndStatus(USER_ID,
				VerificationType.FRESHMAN, VerificationStatus.PENDING)).willReturn(true);

		verificationService.createVerification(USER_ID, LOW_INCOME_REQUEST, NOW);

		assertThat(savedVerification().getVerificationType()).isEqualTo(VerificationType.LOW_INCOME);
	}

	@Test
	void 회원이_없으면_AUTH_USER_NOT_FOUND() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID, LOW_INCOME_REQUEST, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_USER_NOT_FOUND);
	}

	@Test
	void 동네_인증을_하면_심사_없이_바로_APPROVED로_저장한다() {
		givenUserExists();

		VerificationCreateResponse response = verificationService.verifyNeighborhood(USER_ID, NOW);

		PriorityVerification saved = savedVerification();
		assertThat(saved.getUser().getId()).isEqualTo(USER_ID);
		assertThat(saved.getVerificationType()).isEqualTo(VerificationType.NEIGHBORHOOD);
		assertThat(saved.getStatus()).isEqualTo(VerificationStatus.APPROVED);
		assertThat(saved.getDocumentType()).isNull();
		assertThat(saved.getSubmittedAt()).isEqualTo(NOW);
		assertThat(saved.getReviewedAt()).isNull();
		assertThat(saved.getExpiresAt()).isNull();
		assertThat(response.verificationType()).isEqualTo(VerificationType.NEIGHBORHOOD);
		assertThat(response.status()).isEqualTo(VerificationStatus.APPROVED);
	}

	@Test
	void 동네_인증이_이미_있으면_거절한다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.NEIGHBORHOOD, null)));

		assertThatThrownBy(() -> verificationService.verifyNeighborhood(USER_ID, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
		then(priorityVerificationRepository).should(never()).save(any());
	}

	@Test
	void 우선배정_인증만_있으면_동네_인증을_할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, null)));

		verificationService.verifyNeighborhood(USER_ID, NOW);

		assertThat(savedVerification().getVerificationType()).isEqualTo(VerificationType.NEIGHBORHOOD);
	}

	@Test
	void 동네_인증_회원이_없으면_AUTH_USER_NOT_FOUND() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> verificationService.verifyNeighborhood(USER_ID, NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_USER_NOT_FOUND);
	}

	@Test
	void 동네_인증이_승인돼_있으면_신청_자격이_있다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.NEIGHBORHOOD, null)));

		assertThat(verificationService.hasNeighborhoodVerification(USER_ID, NOW)).isTrue();
	}

	@Test
	void 동네_인증_없이_우선배정만_승인돼_있으면_신청_자격이_없다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, null),
				approved(2L, VerificationType.LOW_INCOME, null)));

		assertThat(verificationService.hasNeighborhoodVerification(USER_ID, NOW)).isFalse();
	}

	@Test
	void 동네_인증이_만료됐으면_신청_자격이_없다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.NEIGHBORHOOD, NOW.minusSeconds(1))));

		assertThat(verificationService.hasNeighborhoodVerification(USER_ID, NOW)).isFalse();
	}

	@Test
	void 우선배정_인증이_없으면_0점() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.NEIGHBORHOOD, null),
				verification(2L, VerificationType.FRESHMAN, VerificationStatus.PENDING, SUBMITTED_AT),
				verification(3L, VerificationType.LOW_INCOME, VerificationStatus.REJECTED, SUBMITTED_AT)));

		assertThat(verificationService.calculatePriorityScore(USER_ID, NOW)).isZero();
	}

	@Test
	void 신입생이나_기초수급자_중_하나가_승인돼_있으면_1점() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, NOW.plusDays(30))));

		assertThat(verificationService.calculatePriorityScore(USER_ID, NOW)).isEqualTo(1);
	}

	@Test
	void 둘_다_승인돼_있어도_1점() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, null),
				approved(2L, VerificationType.LOW_INCOME, null)));

		assertThat(verificationService.calculatePriorityScore(USER_ID, NOW)).isEqualTo(1);
	}

	@Test
	void 우선배정_승인이_만료됐으면_0점() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.LOW_INCOME, NOW.minusSeconds(1))));

		assertThat(verificationService.calculatePriorityScore(USER_ID, NOW)).isZero();
	}

	@Test
	void 신청한_적_없으면_빈_목록() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of());

		assertThat(verificationService.getMyVerifications(USER_ID, NOW)).isEmpty();
	}

	@Test
	void 신청한_적_있는_유형만_서류_종류와_함께_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(1L, VerificationType.FRESHMAN, VerificationStatus.PENDING, SUBMITTED_AT)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses).extracting(MyVerificationResponse::verificationType)
				.containsExactly(VerificationType.FRESHMAN);
		assertThat(responses.get(0).documentType()).isEqualTo(DocumentType.STUDENT_ID_CARD);
	}

	@Test
	void 유형마다_가장_최근_제출_건_기준으로_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(2L, VerificationType.FRESHMAN, VerificationStatus.PENDING, SUBMITTED_AT.plusDays(5)),  // 재신청
				verification(1L, VerificationType.FRESHMAN, VerificationStatus.REJECTED, SUBMITTED_AT),
				verification(3L, VerificationType.NEIGHBORHOOD, VerificationStatus.APPROVED, SUBMITTED_AT.plusDays(1))));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).verificationType()).isEqualTo(VerificationType.NEIGHBORHOOD);
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
	void 유형_순서는_NEIGHBORHOOD_FRESHMAN_LOW_INCOME() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(1L, VerificationType.LOW_INCOME, VerificationStatus.PENDING, SUBMITTED_AT),
				verification(2L, VerificationType.FRESHMAN, VerificationStatus.PENDING, SUBMITTED_AT),
				verification(3L, VerificationType.NEIGHBORHOOD, VerificationStatus.APPROVED, SUBMITTED_AT)));

		assertThat(verificationService.getMyVerifications(USER_ID, NOW))
				.extracting(MyVerificationResponse::verificationType)
				.containsExactly(VerificationType.NEIGHBORHOOD, VerificationType.FRESHMAN, VerificationType.LOW_INCOME);
	}

	@Test
	void 반려_사유는_REJECTED일_때만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				reviewed(1L, VerificationType.FRESHMAN, VerificationStatus.REJECTED),
				reviewed(2L, VerificationType.LOW_INCOME, VerificationStatus.APPROVED)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).rejectionReason()).isEqualTo("반려 사유");   // FRESHMAN, REJECTED
		assertThat(responses.get(1).rejectionReason()).isNull();                 // LOW_INCOME, APPROVED
	}

	@Test
	void 만료_일시는_APPROVED일_때만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				reviewed(1L, VerificationType.FRESHMAN, VerificationStatus.REJECTED),
				reviewed(2L, VerificationType.NEIGHBORHOOD, VerificationStatus.APPROVED),
				reviewed(3L, VerificationType.LOW_INCOME, VerificationStatus.EXPIRED)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).expiresAt()).isEqualTo(SUBMITTED_AT.plusMonths(6));  // NEIGHBORHOOD, APPROVED
		assertThat(responses.get(1).expiresAt()).isNull();                               // FRESHMAN, REJECTED
		assertThat(responses.get(2).expiresAt()).isNull();                               // LOW_INCOME, EXPIRED
	}

	@Test
	void 만료일이_지난_승인은_EXPIRED로_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.FRESHMAN, NOW.minusSeconds(1)),     // 만료됨
				approved(2L, VerificationType.NEIGHBORHOOD, NOW.plusSeconds(1)),  // 아직 유효
				approved(3L, VerificationType.LOW_INCOME, null)));                // 만료일 없음

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).status()).isEqualTo(VerificationStatus.APPROVED);   // NEIGHBORHOOD
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

	private void givenUserExists() {
		User user = User.builder().kakaoId("kakao-1").nickname("월계1동 이웃").build();
		ReflectionTestUtils.setField(user, "id", USER_ID);
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
		// DB가 id를 채워주는 것을 흉내낸다
		given(priorityVerificationRepository.save(any())).willAnswer(invocation -> {
			PriorityVerification verification = invocation.getArgument(0);
			ReflectionTestUtils.setField(verification, "id", 10L);
			return verification;
		});
	}

	private PriorityVerification savedVerification() {
		ArgumentCaptor<PriorityVerification> captor = ArgumentCaptor.forClass(PriorityVerification.class);
		then(priorityVerificationRepository).should().save(captor.capture());
		return captor.getValue();
	}

	private static PriorityVerification verification(Long id, VerificationType type, VerificationStatus status,
			LocalDateTime submittedAt) {
		PriorityVerification verification = PriorityVerification.builder()
				.verificationType(type)
				.documentType(documentTypeOf(type))
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

	private static DocumentType documentTypeOf(VerificationType type) {
		return switch (type) {
			case NEIGHBORHOOD -> null;
			case FRESHMAN -> DocumentType.STUDENT_ID_CARD;
			case LOW_INCOME -> DocumentType.RECIPIENT_CERTIFICATE;
		};
	}

}
