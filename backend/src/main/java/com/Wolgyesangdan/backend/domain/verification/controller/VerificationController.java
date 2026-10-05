package com.Wolgyesangdan.backend.domain.verification.controller;

import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verifications")
@RequiredArgsConstructor
public class VerificationController {

	private final VerificationService verificationService;

	@GetMapping("/me")
	public List<MyVerificationResponse> getMyVerifications(@AuthenticationPrincipal Long userId) {
		return verificationService.getMyVerifications(userId);
	}

}
