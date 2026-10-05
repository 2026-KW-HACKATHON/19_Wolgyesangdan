package com.Wolgyesangdan.backend.domain.verification.dto;

import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * 우선배정 인증 신청 (신입생·기초수급자). 서류 사진은 받지 않고 서류 종류만 받는다 (UI 스텁, #72).
 * 동네 인증(NEIGHBORHOOD)은 심사가 없어서 별도 API로 신청한다.
 */
public record VerificationCreateRequest(
		@NotNull VerificationType verificationType,
		@NotNull DocumentType documentType) {

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
