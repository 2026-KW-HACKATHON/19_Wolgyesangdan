package com.Wolgyesangdan.backend.domain.user.service;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.user.dto.ContactResponse;
import com.Wolgyesangdan.backend.domain.user.dto.ContactUpdateRequest;
import com.Wolgyesangdan.backend.domain.user.dto.MyInfoResponse;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public MyInfoResponse getMyInfo(Long userId) {
		return MyInfoResponse.from(findUser(userId));
	}

	public ContactResponse getMyContact(Long userId) {
		return ContactResponse.from(findUser(userId));
	}

	@Transactional
	public ContactResponse updateMyContact(Long userId, ContactUpdateRequest request) {
		User user = findUser(userId);
		user.updateContact(request.contactType(), request.phone(), request.openchatLink());
		return ContactResponse.from(user);
	}

	// 유효한 토큰인데 회원이 없는 경우 — 회원 탈퇴 기능이 없어서 운영진이 DB에서 직접 지운 경우뿐. 404가 아니라 401로 다시 로그인하게 한다 (#171)
	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_USER_NOT_FOUND));
	}

}
