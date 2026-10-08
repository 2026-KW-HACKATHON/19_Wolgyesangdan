package com.Wolgyesangdan.backend.domain.inquiry.controller;

import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquiryDetailResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquirySummaryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryAnswerRequest;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;
import com.Wolgyesangdan.backend.domain.inquiry.service.AdminInquiryService;
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

/**
 * 관리자 문의 관리 (ADMIN만 — SecurityConfig의 /admin/**).
 */
@RestController
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

	private final AdminInquiryService adminInquiryService;

	/** 문의 목록 — 답변 대기 우선. status를 안 보내면 전체. page는 0부터, size 기본 20 */
	@GetMapping
	public PageResponse<AdminInquirySummaryResponse> getInquiries(
			@RequestParam(required = false) InquiryStatus status,
			@PageableDefault(size = 20) Pageable pageable) {
		return PageResponse.from(adminInquiryService.getInquiries(status, pageable));
	}

	@GetMapping("/{inquiryId}")
	public AdminInquiryDetailResponse getInquiry(@PathVariable Long inquiryId) {
		return adminInquiryService.getInquiry(inquiryId);
	}

	/** 답변 등록. 이미 답변했으면 409 INQUIRY_ALREADY_ANSWERED */
	@PostMapping("/{inquiryId}/answer")
	public AdminInquiryDetailResponse answer(@AuthenticationPrincipal Long adminId, @PathVariable Long inquiryId,
			@Valid @RequestBody InquiryAnswerRequest request) {
		return adminInquiryService.answer(adminId, inquiryId, request);
	}

}
