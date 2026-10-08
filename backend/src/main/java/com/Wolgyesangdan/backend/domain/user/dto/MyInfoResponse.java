package com.Wolgyesangdan.backend.domain.user.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.domain.user.entity.User;

public record MyInfoResponse(
		Long id,
		String nickname,
		String email,
		ContactType contactType,
		String phone,
		String openchatLink,
		// 관리자 웹이 로그인 뒤 운영자인지 확인하는 데 쓴다
		Role role,
		LocalDateTime createdAt) {

	public static MyInfoResponse from(User user) {
		return new MyInfoResponse(user.getId(), user.getNickname(), user.getEmail(), user.getContactType(),
				user.getPhone(), user.getOpenchatLink(), user.getRole(), user.getCreatedAt());
	}

}
