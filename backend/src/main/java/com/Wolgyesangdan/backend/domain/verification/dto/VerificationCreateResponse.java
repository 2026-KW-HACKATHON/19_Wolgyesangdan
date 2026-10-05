package com.Wolgyesangdan.backend.domain.verification.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;

public record VerificationCreateResponse(
		Long id,
		VerificationType verificationType,
		VerificationStatus status,
		LocalDateTime submittedAt) {

	public static VerificationCreateResponse from(PriorityVerification verification) {
		return new VerificationCreateResponse(verification.getId(), verification.getVerificationType(),
				verification.getStatus(), verification.getSubmittedAt());
	}

}
