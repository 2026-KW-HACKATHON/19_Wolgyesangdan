package com.Wolgyesangdan.backend.domain.application.controller;

import com.Wolgyesangdan.backend.domain.application.dto.ApplicationCreateResponse;
import com.Wolgyesangdan.backend.domain.application.dto.MyApplicationSummaryResponse;
import com.Wolgyesangdan.backend.domain.application.service.ApplicationService;
import com.Wolgyesangdan.backend.global.dto.PageResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

	private static final int MAX_PAGE_SIZE = 100;

	private final ApplicationService applicationService;

	/** 물품 신청 (로그인 필요) */
	@PostMapping("/items/{itemId}/applications")
	public ResponseEntity<ApplicationCreateResponse> apply(@AuthenticationPrincipal Long userId,
			@PathVariable Long itemId) {
		ApplicationCreateResponse response = applicationService.apply(userId, itemId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/** 신청 취소 (로그인 필요) — WAITING 상태일 때만 가능 */
	@DeleteMapping("/applications/{applicationId}")
	public ResponseEntity<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long applicationId) {
		applicationService.cancel(userId, applicationId);
		return ResponseEntity.noContent().build();
	}

	/** 내가 신청한 물품 (로그인 필요). 최근 신청순 */
	@GetMapping("/users/me/applications")
	public PageResponse<MyApplicationSummaryResponse> getMyApplications(@AuthenticationPrincipal Long userId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(
				applicationService.getMyApplications(userId, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
	}

}
