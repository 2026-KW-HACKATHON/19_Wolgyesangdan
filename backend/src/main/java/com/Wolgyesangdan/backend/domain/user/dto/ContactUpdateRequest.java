package com.Wolgyesangdan.backend.domain.user.dto;

import com.Wolgyesangdan.backend.domain.user.entity.ContactType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 형식 규칙은 프론트 ContactSettingsPage의 검증과 동일하게 맞춘다.
 * contactType으로 고른 쪽 값만 필수이고, 다른 쪽 값은 보내도 무시된다.
 */
public record ContactUpdateRequest(
		@NotNull ContactType contactType,
		@Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-0000-0000 형식이어야 합니다.")
		String phone,
		@Pattern(regexp = "^https://open\\.kakao\\.com/o/[A-Za-z0-9]+$", message = "오픈채팅 링크는 https://open.kakao.com/o/... 형식이어야 합니다.")
		String openchatLink) {

	@AssertTrue(message = "선택한 연락 수단의 값을 입력해주세요.")
	public boolean isValueProvidedForContactType() {
		if (contactType == null) {
			return true; // contactType 누락은 @NotNull이 따로 잡는다
		}
		return switch (contactType) {
			case PHONE -> phone != null;
			case OPENCHAT -> openchatLink != null;
		};
	}

}
