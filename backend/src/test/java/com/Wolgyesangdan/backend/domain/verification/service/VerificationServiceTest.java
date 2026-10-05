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

import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.exception.CommonErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class VerificationServiceTest {

	private static final Long USER_ID = 1L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 15, 0);
	private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 9, 10, 9, 0);

	private final PriorityVerificationRepository priorityVerificationRepository = Mockito
			.mock(PriorityVerificationRepository.class);
	private final UserRepository userRepository = Mockito.mock(UserRepository.class);
	private final VerificationService verificationService = new VerificationService(priorityVerificationRepository,
			userRepository);

	@Test
	void 학생_인증을_신청하면_학번에서_입학연도를_계산해_PENDING으로_저장한다() {
		givenUserExists();

		VerificationCreateResponse response = verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.STUDENT, " 202312345 ", " 산업디자인 ", null, null), NOW);

		PriorityVerification saved = savedVerification();
		assertThat(saved.getUser().getId()).isEqualTo(USER_ID);
		assertThat(saved.getVerificationType()).isEqualTo(VerificationType.STUDENT);
		assertThat(saved.getStatus()).isEqualTo(VerificationStatus.PENDING);
		assertThat(saved.getStudentId()).isEqualTo("202312345");
		assertThat(saved.getDepartment()).isEqualTo("산업디자인");
		assertThat(saved.getAdmissionYear()).isEqualTo(2023);
		assertThat(saved.getSubmittedAt()).isEqualTo(NOW);
		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.verificationType()).isEqualTo(VerificationType.STUDENT);
		assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);
		assertThat(response.submittedAt()).isEqualTo(NOW);
	}

	@Test
	void 주민_인증을_신청하면_이름과_주소를_저장한다() {
		givenUserExists();

		verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.RESIDENT, null, null, "정하늘", "서울 노원구 월계로 1"), NOW);

		PriorityVerification saved = savedVerification();
		assertThat(saved.getStatus()).isEqualTo(VerificationStatus.PENDING);
		assertThat(saved.getName()).isEqualTo("정하늘");
		assertThat(saved.getAddress()).isEqualTo("서울 노원구 월계로 1");
		assertThat(saved.getStudentId()).isNull();
		assertThat(saved.getAdmissionYear()).isNull();
	}

	@Test
	void 저소득층_인증은_유형만_저장하고_다른_유형의_값은_무시한다() {
		givenUserExists();

		verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.LOW_INCOME, "202312345", "산업디자인", "정하늘", "주소"), NOW);

		PriorityVerification saved = savedVerification();
		assertThat(saved.getVerificationType()).isEqualTo(VerificationType.LOW_INCOME);
		assertThat(saved.getStatus()).isEqualTo(VerificationStatus.PENDING);
		assertThat(saved.getStudentId()).isNull();
		assertThat(saved.getDepartment()).isNull();
		assertThat(saved.getAdmissionYear()).isNull();
		assertThat(saved.getName()).isNull();
		assertThat(saved.getAddress()).isNull();
	}

	@Test
	void 같은_유형에_심사_중인_신청이_있으면_거절한다() {
		givenUserExists();
		given(priorityVerificationRepository.existsByUserIdAndVerificationTypeAndStatus(USER_ID,
				VerificationType.LOW_INCOME, VerificationStatus.PENDING)).willReturn(true);

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.LOW_INCOME, null, null, null, null), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_PENDING);
		then(priorityVerificationRepository).should(never()).save(any());
	}

	@Test
	void 같은_유형에_유효한_승인이_있으면_거절한다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.STUDENT, NOW.plusSeconds(1))));

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.STUDENT, "202312345", "산업디자인", null, null), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
		then(priorityVerificationRepository).should(never()).save(any());
	}

	@Test
	void 만료일이_없는_승인이_있어도_거절한다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.LOW_INCOME, null)));

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.LOW_INCOME, null, null, null, null), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
	}

	@Test
	void 승인이_만료됐으면_다시_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.STUDENT, NOW.minusSeconds(1))));

		verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.STUDENT, "202312345", "산업디자인", null, null), NOW);

		assertThat(savedVerification().getStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void 승인_뒤에_낸_재신청이_반려됐으면_다시_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.STUDENT, NOW.minusDays(1)),  // 만료된 예전 승인
				verification(2L, VerificationType.STUDENT, VerificationStatus.REJECTED, SUBMITTED_AT.plusDays(5))));

		verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.STUDENT, "202312345", "산업디자인", null, null), NOW);

		assertThat(savedVerification().getStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void 다른_유형이_승인돼_있어도_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.RESIDENT, NOW.plusDays(30))));

		verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.LOW_INCOME, null, null, null, null), NOW);

		assertThat(savedVerification().getVerificationType()).isEqualTo(VerificationType.LOW_INCOME);
	}

	@Test
	void 다른_유형이_심사_중이어도_신청할_수_있다() {
		givenUserExists();
		given(priorityVerificationRepository.existsByUserIdAndVerificationTypeAndStatus(USER_ID,
				VerificationType.STUDENT, VerificationStatus.PENDING)).willReturn(true);

		verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.LOW_INCOME, null, null, null, null), NOW);

		assertThat(savedVerification().getVerificationType()).isEqualTo(VerificationType.LOW_INCOME);
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc12345", "2023-1234", "202", "2023123456789", "1933123456", "2027123456", "0000123456"})
	void 학번에서_입학연도를_계산할_수_없으면_거절한다(String studentId) {
		givenUserExists();

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.STUDENT, studentId, "산업디자인", null, null), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_INVALID_STUDENT_ID);
		then(priorityVerificationRepository).should(never()).save(any());
	}

	@Test
	void 올해_입학한_학번은_신청할_수_있다() {
		givenUserExists();

		verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.STUDENT, "2026123456", "산업디자인", null, null), NOW);

		assertThat(savedVerification().getAdmissionYear()).isEqualTo(2026);
	}

	@Test
	void 회원이_없으면_NOT_FOUND() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> verificationService.createVerification(USER_ID,
				new VerificationCreateRequest(VerificationType.LOW_INCOME, null, null, null, null), NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(CommonErrorCode.NOT_FOUND);
	}

	@Test
	void 신청한_적_없으면_빈_목록() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of());

		assertThat(verificationService.getMyVerifications(USER_ID, NOW)).isEmpty();
	}

	@Test
	void 신청한_적_있는_유형만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(1L, VerificationType.STUDENT, VerificationStatus.PENDING, SUBMITTED_AT)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses).extracting(MyVerificationResponse::verificationType)
				.containsExactly(VerificationType.STUDENT);
	}

	@Test
	void 유형마다_가장_최근_제출_건_기준으로_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(2L, VerificationType.STUDENT, VerificationStatus.PENDING, SUBMITTED_AT.plusDays(5)),  // 재신청
				verification(1L, VerificationType.STUDENT, VerificationStatus.REJECTED, SUBMITTED_AT),
				verification(3L, VerificationType.RESIDENT, VerificationStatus.APPROVED, SUBMITTED_AT.plusDays(1))));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).verificationType()).isEqualTo(VerificationType.RESIDENT);
		assertThat(responses.get(0).status()).isEqualTo(VerificationStatus.APPROVED);
		assertThat(responses.get(1).verificationType()).isEqualTo(VerificationType.STUDENT);
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
	void 유형_순서는_RESIDENT_STUDENT_LOW_INCOME() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				verification(1L, VerificationType.LOW_INCOME, VerificationStatus.PENDING, SUBMITTED_AT),
				verification(2L, VerificationType.STUDENT, VerificationStatus.PENDING, SUBMITTED_AT),
				verification(3L, VerificationType.RESIDENT, VerificationStatus.PENDING, SUBMITTED_AT)));

		assertThat(verificationService.getMyVerifications(USER_ID, NOW))
				.extracting(MyVerificationResponse::verificationType)
				.containsExactly(VerificationType.RESIDENT, VerificationType.STUDENT, VerificationType.LOW_INCOME);
	}

	@Test
	void 반려_사유는_REJECTED일_때만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				reviewed(1L, VerificationType.STUDENT, VerificationStatus.REJECTED),
				reviewed(2L, VerificationType.RESIDENT, VerificationStatus.APPROVED)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).rejectionReason()).isNull();                 // RESIDENT, APPROVED
		assertThat(responses.get(1).rejectionReason()).isEqualTo("반려 사유");   // STUDENT, REJECTED
	}

	@Test
	void 만료_일시는_APPROVED일_때만_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				reviewed(1L, VerificationType.STUDENT, VerificationStatus.REJECTED),
				reviewed(2L, VerificationType.RESIDENT, VerificationStatus.APPROVED),
				reviewed(3L, VerificationType.LOW_INCOME, VerificationStatus.EXPIRED)));

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).expiresAt()).isEqualTo(SUBMITTED_AT.plusMonths(6));  // RESIDENT, APPROVED
		assertThat(responses.get(1).expiresAt()).isNull();                               // STUDENT, REJECTED
		assertThat(responses.get(2).expiresAt()).isNull();                               // LOW_INCOME, EXPIRED
	}

	@Test
	void 만료일이_지난_승인은_EXPIRED로_내려준다() {
		given(priorityVerificationRepository.findByUserId(USER_ID)).willReturn(List.of(
				approved(1L, VerificationType.STUDENT, NOW.minusSeconds(1)),   // 만료됨
				approved(2L, VerificationType.RESIDENT, NOW.plusSeconds(1)),   // 아직 유효
				approved(3L, VerificationType.LOW_INCOME, null)));             // 만료일 없음

		List<MyVerificationResponse> responses = verificationService.getMyVerifications(USER_ID, NOW);

		assertThat(responses.get(0).status()).isEqualTo(VerificationStatus.APPROVED);   // RESIDENT
		assertThat(responses.get(0).expiresAt()).isEqualTo(NOW.plusSeconds(1));
		assertThat(responses.get(1).status()).isEqualTo(VerificationStatus.EXPIRED);    // STUDENT
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
