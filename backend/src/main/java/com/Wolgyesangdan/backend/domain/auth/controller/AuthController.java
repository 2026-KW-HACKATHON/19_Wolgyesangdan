package com.Wolgyesangdan.backend.domain.auth.controller;

import com.Wolgyesangdan.backend.domain.auth.dto.AdminLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.KakaoLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.LoginResponse;
import com.Wolgyesangdan.backend.domain.auth.dto.TokenRefreshRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.TokenResponse;
import com.Wolgyesangdan.backend.domain.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/kakao")
	public LoginResponse kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
		return authService.kakaoLogin(request);
	}

	/** 관리자 웹 로그인 (아이디·비밀번호). 응답은 카카오 로그인과 같은 형식 */
	@PostMapping("/admin/login")
	public LoginResponse adminLogin(@Valid @RequestBody AdminLoginRequest request) {
		return authService.adminLogin(request);
	}

	@PostMapping("/token/refresh")
	public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
		return authService.refresh(request);
	}

}
