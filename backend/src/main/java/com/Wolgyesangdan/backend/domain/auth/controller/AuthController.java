package com.Wolgyesangdan.backend.domain.auth.controller;

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

	@PostMapping("/token/refresh")
	public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
		return authService.refresh(request);
	}

}
