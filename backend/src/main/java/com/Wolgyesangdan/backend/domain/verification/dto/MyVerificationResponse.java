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

	// status는 조회 시점 기준 (만료일이 지난 승인은 EXPIRED). rejectionReason은 REJECTED, expiresAt은 APPROVED일 때만 내려준다
	public static MyVerificationResponse from(PriorityVerification verification, LocalDateTime now) {
		VerificationStatus status = verification.statusAt(now);
		return new MyVerificationResponse(
				verification.getVerificationType(),
				status,
				verification.getSubmittedAt(),
				verification.getReviewedAt(),
				status == VerificationStatus.REJECTED ? verification.getRejectionReason() : null,
				status == VerificationStatus.APPROVED ? verification.getExpiresAt() : null);
	}

}
