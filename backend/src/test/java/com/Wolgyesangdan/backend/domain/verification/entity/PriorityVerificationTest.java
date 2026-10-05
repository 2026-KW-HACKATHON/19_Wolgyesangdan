package com.Wolgyesangdan.backend.domain.verification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PriorityVerificationTest {

	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2027, 2, 28, 23, 59, 59);

	@Test
	void 승인이라도_만료일이_지나면_EXPIRED() {
		PriorityVerification verification = verification(VerificationStatus.APPROVED, EXPIRES_AT);

		assertThat(verification.statusAt(EXPIRES_AT.minusSeconds(1))).isEqualTo(VerificationStatus.APPROVED);
		assertThat(verification.statusAt(EXPIRES_AT)).isEqualTo(VerificationStatus.APPROVED); // 만료 시각 정각까지 유효
		assertThat(verification.statusAt(EXPIRES_AT.plusSeconds(1))).isEqualTo(VerificationStatus.EXPIRED);
	}

	@Test
	void 만료일이_없는_승인은_계속_APPROVED() {
		PriorityVerification verification = verification(VerificationStatus.APPROVED, null);

		assertThat(verification.statusAt(EXPIRES_AT.plusYears(10))).isEqualTo(VerificationStatus.APPROVED);
	}

	@Test
	void 승인이_아닌_상태는_만료일과_무관하게_그대로() {
		LocalDateTime afterExpiry = EXPIRES_AT.plusSeconds(1);

		assertThat(verification(VerificationStatus.PENDING, EXPIRES_AT).statusAt(afterExpiry))
				.isEqualTo(VerificationStatus.PENDING);
		assertThat(verification(VerificationStatus.REJECTED, EXPIRES_AT).statusAt(afterExpiry))
				.isEqualTo(VerificationStatus.REJECTED);
		assertThat(verification(VerificationStatus.EXPIRED, EXPIRES_AT).statusAt(afterExpiry))
				.isEqualTo(VerificationStatus.EXPIRED);
	}

	private static PriorityVerification verification(VerificationStatus status, LocalDateTime expiresAt) {
		return PriorityVerification.builder()
				.verificationType(VerificationType.STUDENT)
				.status(status)
				.expiresAt(expiresAt)
				.build();
	}

}
