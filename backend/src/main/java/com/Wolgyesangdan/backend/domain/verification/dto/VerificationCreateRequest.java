package com.Wolgyesangdan.backend.domain.verification.dto;

import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * verificationType에 따라 필수 값이 다르다 — STUDENT는 학번·학과, RESIDENT는 서류상 이름·거주지 주소.
 * LOW_INCOME은 유형 외에 받는 값이 없고, 해당 유형이 아닌 값은 보내도 무시된다.
 * 학번 형식(입학연도 파싱)은 에러 코드가 따로라서 서비스에서 검사한다.
 */
public record VerificationCreateRequest(
		@NotNull VerificationType verificationType,
		@Size(max = 30, message = "학번은 30자 이하여야 합니다.")
		String studentId,
		@Size(max = 50, message = "학과는 50자 이하여야 합니다.")
		String department,
		@Size(max = 50, message = "이름은 50자 이하여야 합니다.")
		String name,
		@Size(max = 255, message = "주소는 255자 이하여야 합니다.")
		String address) {

	@AssertTrue(message = "선택한 인증 유형의 필수 값을 입력해주세요.")
	public boolean isValueProvidedForVerificationType() {
		if (verificationType == null) {
			return true; // verificationType 누락은 @NotNull이 따로 잡는다
		}
		return switch (verificationType) {
			case STUDENT -> hasText(studentId) && hasText(department);
			case RESIDENT -> hasText(name) && hasText(address);
			case LOW_INCOME -> true;
		};
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

}
