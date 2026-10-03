package com.Wolgyesangdan.backend.domain.user.service;

import com.Wolgyesangdan.backend.domain.user.dto.MyInfoResponse;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public MyInfoResponse getMyInfo(Long userId) {
		// 유효한 토큰인데 회원이 없는 경우 — 회원 탈퇴 기능이 없어서 운영진이 DB에서 직접 지운 경우뿐
		return userRepository.findById(userId)
				.map(MyInfoResponse::from)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
	}

}
