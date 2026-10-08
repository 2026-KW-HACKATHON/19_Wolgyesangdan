package com.Wolgyesangdan.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로컬 개발용 임시 로그인 요청. kakaoId가 같으면 같은 사용자로 로그인되고, 없으면 새로 만든다.
 * nickname·admin은 새로 만들 때만 쓰인다 — admin이 true면 관리자(ADMIN)로 만든다. 관리자 테스트는 새 kakaoId로.
 */
public record DevLoginRequest(@NotBlank String kakaoId, @NotBlank String nickname, Boolean admin) {
}
