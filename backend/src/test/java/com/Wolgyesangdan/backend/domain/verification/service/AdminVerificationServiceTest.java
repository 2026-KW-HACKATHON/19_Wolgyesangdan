package com.Wolgyesangdan.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationApproveRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationDetailResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationFileResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationAuditAction;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationAuditLog;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;
import com.Wolgyesangdan.backend.domain.verification.repository.VerificationAuditLogRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class AdminVerificationServiceTest {

	private static final Long ADMIN_ID = 9L;
	private static final Long VERIFICATION_ID = 10L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 14, 0);
	private static final String FILE_KEY = "verifications/2026/10/08/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.pdf";

	private final PriorityVerificationRepository priorityVerificationRepository =
			Mockito.mock(PriorityVerificationRepository.class);
	private final VerificationAuditLogRepository verificationAuditLogRepository =
			Mockito.mock(VerificationAuditLogRepository.class);
	private final UserRepository userRepository = Mockito.mock(UserRepository.class);
	private final VerificationService verificationService = Mockito.mock(VerificationService.class);
	private final VerificationDocumentStorage verificationDocumentStorage = Mockito.mock(VerificationDocumentStorage.class);
	private final AdminVerificationService service = new AdminVerificationService(priorityVerificationRepository,
			verificationAuditLogRepository, userRepository, verificationService, verificationDocumentStorage);

	private final User applicant = withId(User.builder().kakaoId("1").nickname("하늘").build(), 1L);
	private final User admin = withId(User.builder().kakaoId("admin:admin").nickname("운영자").role(Role.ADMIN).build(),
			ADMIN_ID);

	@BeforeEach
	void setUp() {
		given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin));
		given(verificationService.hasNeighborhoodVerification(1L)).willReturn(true);
	}

	@Test
	void 신입생을_승인하면_입학한_해_12월_31일까지_유효하다() {
		PriorityVerification verification = givenPending(VerificationType.FRESHMAN);

		AdminVerificationDetailResponse response =
				service.approve(ADMIN_ID, VERIFICATION_ID, new AdminVerificationApproveRequest(2027), NOW);

		assertThat(verification.getStatus()).isEqualTo(VerificationStatus.APPROVED);
		assertThat(verification.getExpiresAt()).isEqualTo(LocalDateTime.of(2027, 12, 31, 23, 59, 59));
		assertThat(verification.getReviewedAt()).isEqualTo(NOW);
		assertThat(verification.getReviewedBy()).isSameAs(admin);
		assertThat(response.reviewerNickname()).isEqualTo("운영자");
		assertThat(response.neighborhoodVerified()).isTrue();
		assertThat(savedAudit().getAction()).isEqualTo(VerificationAuditAction.APPROVE);
	}

	@Test
	void 신입생_승인에_입학_연도가_없거나_올해_내년이_아니면_400() {
		givenPending(VerificationType.FRESHMAN);

		for (AdminVerificationApproveRequest request : new AdminVerificationApproveRequest[] {null,
				new AdminVerificationApproveRequest(null), new AdminVerificationApproveRequest(2025),
				new AdminVerificationApproveRequest(2028)}) {
			assertThatThrownBy(() -> service.approve(ADMIN_ID, VERIFICATION_ID, request, NOW))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ADMISSION_YEAR_INVALID);
		}
		then(verificationAuditLogRepository).should(never()).save(any());
	}

	@Test
	void 기초수급자를_승인하면_승인_시각부터_1년간_유효하다() {
		PriorityVerification verification = givenPending(VerificationType.LOW_INCOME);

		service.approve(ADMIN_ID, VERIFICATION_ID, null, NOW);

		assertThat(verification.getStatus()).isEqualTo(VerificationStatus.APPROVED);
		assertThat(verification.getExpiresAt()).isEqualTo(LocalDateTime.of(2027, 10, 8, 14, 0));
	}

	@Test
	void 반려하면_사유를_저장하고_감사_로그를_남긴다() {
		PriorityVerification verification = givenPending(VerificationType.LOW_INCOME);

		AdminVerificationDetailResponse response =
				service.reject(ADMIN_ID, VERIFICATION_ID, " 이름이 보이지 않아요 ", NOW);

		assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
		assertThat(verification.getRejectionReason()).isEqualTo("이름이 보이지 않아요");
		assertThat(response.rejectionReason()).isEqualTo("이름이 보이지 않아요");
		assertThat(savedAudit().getAction()).isEqualTo(VerificationAuditAction.REJECT);
	}

	@Test
	void 이미_처리된_신청은_다시_승인_반려할_수_없다() {
		PriorityVerification verification = givenPending(VerificationType.LOW_INCOME);
		verification.reject(admin, NOW.minusDays(1), "흐려요");

		assertThatThrownBy(() -> service.approve(ADMIN_ID, VERIFICATION_ID, null, NOW))
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_REVIEWED);
		assertThatThrownBy(() -> service.reject(ADMIN_ID, VERIFICATION_ID, "다시", NOW))
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_ALREADY_REVIEWED);
	}

	@Test
	void 동네_인증이나_없는_신청은_404() {
		given(priorityVerificationRepository.findWithUsersById(VERIFICATION_ID)).willReturn(Optional.of(
				verification(VerificationType.NEIGHBORHOOD, VerificationStatus.APPROVED)));
		given(priorityVerificationRepository.findWithUsersById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.getVerification(VERIFICATION_ID))
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_NOT_FOUND);
		assertThatThrownBy(() -> service.getVerification(99L))
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_NOT_FOUND);
	}

	@Test
	void 서류를_열람하면_임시_URL을_주고_열람_기록을_남긴다() {
		givenPending(VerificationType.FRESHMAN);
		AdminVerificationFileResponse file =
				new AdminVerificationFileResponse("https://signed", "application/pdf", NOW.plusMinutes(5));
		given(verificationDocumentStorage.issueViewUrl(any(), any())).willReturn(file);

		assertThat(service.issueFileUrl(ADMIN_ID, VERIFICATION_ID)).isEqualTo(file);
		then(verificationDocumentStorage).should().issueViewUrl(any(), any());
		VerificationAuditLog audit = savedAudit();
		assertThat(audit.getAction()).isEqualTo(VerificationAuditAction.VIEW_FILE);
		assertThat(audit.getAdmin()).isSameAs(admin);
	}

	@Test
	void 서류_파일이_없으면_열람할_수_없다() {
		PriorityVerification verification = givenPending(VerificationType.FRESHMAN);
		verification.purgeDocument();

		assertThatThrownBy(() -> service.issueFileUrl(ADMIN_ID, VERIFICATION_ID))
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_DOCUMENT_NOT_FOUND);
		then(verificationAuditLogRepository).should(never()).save(any());
	}

	private PriorityVerification givenPending(VerificationType type) {
		PriorityVerification verification = verification(type, VerificationStatus.PENDING);
		given(priorityVerificationRepository.findWithUsersById(VERIFICATION_ID)).willReturn(Optional.of(verification));
		return verification;
	}

	private PriorityVerification verification(VerificationType type, VerificationStatus status) {
		return withId(PriorityVerification.builder()
				.user(applicant)
				.verificationType(type)
				.documentType(type == VerificationType.FRESHMAN ? DocumentType.ADMISSION_LETTER
						: type == VerificationType.LOW_INCOME ? DocumentType.RECIPIENT_CERTIFICATE : null)
				.fileKey(type == VerificationType.NEIGHBORHOOD ? null : FILE_KEY)
				.applicantName("김하늘")
				.status(status)
				.submittedAt(NOW.minusDays(1))
				.build(), VERIFICATION_ID);
	}

	private VerificationAuditLog savedAudit() {
		ArgumentCaptor<VerificationAuditLog> captor = ArgumentCaptor.forClass(VerificationAuditLog.class);
		then(verificationAuditLogRepository).should().save(captor.capture());
		return captor.getValue();
	}

	private static <T> T withId(T entity, Long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}

}
