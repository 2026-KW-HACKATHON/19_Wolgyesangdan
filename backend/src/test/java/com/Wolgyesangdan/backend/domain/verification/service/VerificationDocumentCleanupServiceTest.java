package com.Wolgyesangdan.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.exception.SdkClientException;

class VerificationDocumentCleanupServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 3, 0);
	private static final String FILE_KEY = "verifications/2026/10/08/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.jpg";

	private final PriorityVerificationRepository priorityVerificationRepository =
			Mockito.mock(PriorityVerificationRepository.class);
	private final VerificationDocumentStorage verificationDocumentStorage = Mockito.mock(VerificationDocumentStorage.class);
	private final VerificationDocumentCleanupService service =
			new VerificationDocumentCleanupService(priorityVerificationRepository, verificationDocumentStorage);

	@Test
	void 검토_후_30일이_지난_신청을_찾는다() {
		given(verificationDocumentStorage.isAvailable()).willReturn(true);
		given(priorityVerificationRepository.findIdsWithDocumentReviewedBefore(NOW.minusDays(30)))
				.willReturn(List.of(1L, 2L));

		assertThat(service.findIdsToPurge(NOW)).containsExactly(1L, 2L);
	}

	@Test
	void S3_설정이_없으면_지울_수_없으니_찾지_않는다() {
		given(verificationDocumentStorage.isAvailable()).willReturn(false);

		assertThat(service.findIdsToPurge(NOW)).isEmpty();
		then(priorityVerificationRepository).should(never()).findIdsWithDocumentReviewedBefore(any());
	}

	@Test
	void S3_파일을_지우고_서류_정보를_비운다_심사_결과는_남긴다() {
		PriorityVerification verification = reviewed();
		given(priorityVerificationRepository.findById(1L)).willReturn(Optional.of(verification));

		service.purge(1L);

		then(verificationDocumentStorage).should().delete(FILE_KEY);
		assertThat(verification.getFileKey()).isNull();
		assertThat(verification.getApplicantName()).isNull();
		assertThat(verification.getStatus()).isEqualTo(VerificationStatus.APPROVED);
	}

	@Test
	void S3_삭제가_실패하면_서류_정보를_그대로_둬서_다음에_다시_시도한다() {
		PriorityVerification verification = reviewed();
		given(priorityVerificationRepository.findById(1L)).willReturn(Optional.of(verification));
		willThrow(SdkClientException.create("down")).given(verificationDocumentStorage).delete(FILE_KEY);

		assertThatThrownBy(() -> service.purge(1L)).isInstanceOf(SdkClientException.class);
		assertThat(verification.getFileKey()).isEqualTo(FILE_KEY);
		assertThat(verification.getApplicantName()).isEqualTo("김하늘");
	}

	private static PriorityVerification reviewed() {
		return PriorityVerification.builder()
				.verificationType(VerificationType.FRESHMAN)
				.status(VerificationStatus.APPROVED)
				.fileKey(FILE_KEY)
				.applicantName("김하늘")
				.reviewedAt(NOW.minusDays(31))
				.build();
	}

}
