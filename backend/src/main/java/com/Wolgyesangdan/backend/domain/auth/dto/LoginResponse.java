package com.Wolgyesangdan.backend.domain.auth.dto;

import com.Wolgyesangdan.backend.domain.user.entity.User;

public record LoginResponse(String accessToken, String refreshToken, boolean isNewUser, UserInfo user) {

	public static LoginResponse of(TokenResponse tokens, boolean isNewUser, User user) {
		return new LoginResponse(tokens.accessToken(), tokens.refreshToken(), isNewUser,
				new UserInfo(user.getId(), user.getNickname(), user.getEmail()));
	}

	public record UserInfo(Long id, String nickname, String email) {
	}

}
