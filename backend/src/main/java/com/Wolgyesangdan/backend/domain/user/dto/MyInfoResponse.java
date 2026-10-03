package com.Wolgyesangdan.backend.domain.user.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.entity.User;

public record MyInfoResponse(
		Long id,
		String nickname,
		String email,
		ContactType contactType,
		String phone,
		String openchatLink,
		LocalDateTime createdAt) {

	public static MyInfoResponse from(User user) {
		return new MyInfoResponse(user.getId(), user.getNickname(), user.getEmail(), user.getContactType(),
				user.getPhone(), user.getOpenchatLink(), user.getCreatedAt());
	}

}
