package com.Wolgyesangdan.backend.domain.verification.controller;

import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateResponse;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verifications")
@RequiredArgsConstructor
public class VerificationController {

	private final VerificationService verificationService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VerificationCreateResponse createVerification(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody VerificationCreateRequest request) {
		return verificationService.createVerification(userId, request);
	}

	/** GPS 동네 인증 (본문 없음). 심사 없이 바로 APPROVED */
	@PostMapping("/neighborhood")
	@ResponseStatus(HttpStatus.CREATED)
	public VerificationCreateResponse verifyNeighborhood(@AuthenticationPrincipal Long userId) {
		return verificationService.verifyNeighborhood(userId);
	}

	@GetMapping("/me")
	public List<MyVerificationResponse> getMyVerifications(@AuthenticationPrincipal Long userId) {
		return verificationService.getMyVerifications(userId);
	}

}
