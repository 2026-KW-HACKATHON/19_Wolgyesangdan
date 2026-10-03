package com.Wolgyesangdan.backend.domain.auth.controller;

import com.Wolgyesangdan.backend.domain.auth.dto.DevLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.LoginResponse;
import com.Wolgyesangdan.backend.domain.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚠️ 로컬 개발 전용. 카카오 로그인 없이 아무 사용자로나 토큰을 발급하므로 local 프로필에서만 등록된다.
 * 배포 환경은 반드시 local이 아닌 프로필로 띄울 것.
 */
@Profile("local")
@RestController
@RequestMapping("/dev/auth")
@RequiredArgsConstructor
public class DevAuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody DevLoginRequest request) {
		return authService.devLogin(request);
	}

}
