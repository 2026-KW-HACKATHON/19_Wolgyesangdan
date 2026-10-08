package com.Wolgyesangdan.backend.domain.verification.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;

/**
 * 관리자 서류 목록의 한 줄. status는 조회 시점 기준 (만료일이 지난 승인은 EXPIRED).
 * applicantName은 서류가 보관 기간이 지나 지워졌으면 null.
 */
public record AdminVerificationSummaryResponse(
		Long id,
		String applicantName,
		String nickname,
		VerificationType verificationType,
		DocumentType documentType,
		LocalDateTime submittedAt,
		VerificationStatus status) {

	public static AdminVerificationSummaryResponse of(PriorityVerification verification, LocalDateTime now) {
		return new AdminVerificationSummaryResponse(verification.getId(), verification.getApplicantName(),
				verification.getUser().getNickname(), verification.getVerificationType(),
				verification.getDocumentType(), verification.getSubmittedAt(), verification.statusAt(now));
	}

}
