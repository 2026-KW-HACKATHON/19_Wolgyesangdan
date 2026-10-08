package com.Wolgyesangdan.backend.domain.verification.controller;

import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationApproveRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationDetailResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationFileResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationRejectRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationSummaryResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.service.AdminVerificationService;
import com.Wolgyesangdan.backend.global.dto.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 우선배정 서류 심사 (신입생·기초수급자) */
@RestController
@RequestMapping("/admin/verifications")
@RequiredArgsConstructor
public class AdminVerificationController {

	private final AdminVerificationService adminVerificationService;

	/** 서류 목록 — 최근 신청순. status를 안 보내면 전부. page는 0부터, size 기본 20 */
	@GetMapping
	public PageResponse<AdminVerificationSummaryResponse> getVerifications(
			@RequestParam(required = false) VerificationStatus status,
			@PageableDefault(size = 20) Pageable pageable) {
		return PageResponse.from(adminVerificationService.getVerifications(status, pageable));
	}

	@GetMapping("/{verificationId}")
	public AdminVerificationDetailResponse getVerification(@PathVariable Long verificationId) {
		return adminVerificationService.getVerification(verificationId);
	}

	/** 서류 열람용 임시 URL (5분). 열람 기록이 남는다 */
	@GetMapping("/{verificationId}/file")
	public AdminVerificationFileResponse getFileUrl(@AuthenticationPrincipal Long adminId,
			@PathVariable Long verificationId) {
		return adminVerificationService.issueFileUrl(adminId, verificationId);
	}

	/** 승인. 신입생은 본문에 admissionYear 필수, 기초수급자는 본문 없이 */
	@PostMapping("/{verificationId}/approve")
	public AdminVerificationDetailResponse approve(@AuthenticationPrincipal Long adminId,
			@PathVariable Long verificationId,
			@RequestBody(required = false) AdminVerificationApproveRequest request) {
		return adminVerificationService.approve(adminId, verificationId, request);
	}

	@PostMapping("/{verificationId}/reject")
	public AdminVerificationDetailResponse reject(@AuthenticationPrincipal Long adminId,
			@PathVariable Long verificationId,
			@Valid @RequestBody AdminVerificationRejectRequest request) {
		return adminVerificationService.reject(adminId, verificationId, request.reason());
	}

}
