package com.Wolgyesangdan.backend.domain.verification.dto;

import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationDocumentUploadService;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 우선배정 인증 신청 (신입생·기초수급자). 서류 파일은 먼저 POST /verifications/documents/upload-url로 올리고
 * 받은 fileKey를 담는다. applicantName은 서류에 적힌 이름 — 관리자가 서류와 대조한다.
 * 동네 인증(NEIGHBORHOOD)은 심사가 없어서 별도 API로 신청한다.
 */
public record VerificationCreateRequest(
		@NotNull VerificationType verificationType,
		@NotNull DocumentType documentType,
		@NotBlank
		@Pattern(regexp = VerificationDocumentUploadService.FILE_KEY_PATTERN, message = "올바른 서류 파일이 아닙니다.")
		String fileKey,
		@NotBlank @Size(max = 50) String applicantName) {

	@AssertTrue(message = "동네 인증은 여기서 신청할 수 없습니다.")
	public boolean isPriorityVerificationType() {
		return verificationType == null || verificationType.isPriority(); // null은 @NotNull이 따로 잡는다
	}

	@AssertTrue(message = "선택한 인증 유형에 맞는 서류 종류를 선택해주세요.")
	public boolean isDocumentTypeMatchingVerificationType() {
		if (verificationType == null || documentType == null) {
			return true;
		}
		return documentType.getVerificationType() == verificationType;
	}

}
