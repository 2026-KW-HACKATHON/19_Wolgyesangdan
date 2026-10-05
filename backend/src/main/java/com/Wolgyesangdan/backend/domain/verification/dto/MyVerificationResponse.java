package com.Wolgyesangdan.backend.domain.verification.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;

public record MyVerificationResponse(
		VerificationType verificationType,
		VerificationStatus status,
		LocalDateTime submittedAt,
		LocalDateTime reviewedAt,
		String rejectionReason,
		LocalDateTime expiresAt) {

	// rejectionReason은 REJECTED, expiresAt은 APPROVED일 때만 내려준다
	public static MyVerificationResponse from(PriorityVerification verification) {
		VerificationStatus status = verification.getStatus();
		return new MyVerificationResponse(
				verification.getVerificationType(),
				status,
				verification.getSubmittedAt(),
				verification.getReviewedAt(),
				status == VerificationStatus.REJECTED ? verification.getRejectionReason() : null,
				status == VerificationStatus.APPROVED ? verification.getExpiresAt() : null);
	}

}
