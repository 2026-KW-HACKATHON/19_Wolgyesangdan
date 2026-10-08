package com.Wolgyesangdan.backend.domain.verification.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;

/**
 * 관리자 서류 상세. 목록 항목 + 심사 정보.
 * hasDocument가 false면 서류 파일이 없다 (파일 없이 신청된 예전 건, 또는 검토 후 보관 기간이 지나 삭제).
 */
public record AdminVerificationDetailResponse(
		Long id,
		String applicantName,
		String nickname,
		VerificationType verificationType,
		DocumentType documentType,
		LocalDateTime submittedAt,
		VerificationStatus status,
		boolean neighborhoodVerified,
		boolean hasDocument,
		String rejectionReason,
		LocalDateTime reviewedAt,
		String reviewerNickname,
		LocalDateTime expiresAt) {

	public static AdminVerificationDetailResponse of(PriorityVerification verification, boolean neighborhoodVerified,
			LocalDateTime now) {
		User reviewer = verification.getReviewedBy();
		return new AdminVerificationDetailResponse(verification.getId(), verification.getApplicantName(),
				verification.getUser().getNickname(), verification.getVerificationType(),
				verification.getDocumentType(), verification.getSubmittedAt(), verification.statusAt(now),
				neighborhoodVerified, verification.getFileKey() != null, verification.getRejectionReason(),
				verification.getReviewedAt(), reviewer == null ? null : reviewer.getNickname(),
				verification.getExpiresAt());
	}

}
