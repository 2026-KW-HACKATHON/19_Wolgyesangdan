package com.Wolgyesangdan.backend.domain.inquiry.controller;

import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryCreateRequest;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.service.InquiryService;
import com.Wolgyesangdan.backend.global.dto.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 문의 (로그인 필요).
 */
@RestController
@RequiredArgsConstructor
public class InquiryController {

	private final InquiryService inquiryService;

	/** 문의 작성 */
	@PostMapping("/inquiries")
	@ResponseStatus(HttpStatus.CREATED)
	public InquiryResponse createInquiry(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody InquiryCreateRequest request) {
		return inquiryService.createInquiry(userId, request);
	}

	/** 내 문의 — 답변까지 함께, 최근에 쓴 순. page는 0부터, size 기본 20 */
	@GetMapping("/users/me/inquiries")
	public PageResponse<InquiryResponse> getMyInquiries(@AuthenticationPrincipal Long userId,
			@PageableDefault(size = 20) Pageable pageable) {
		return PageResponse.from(inquiryService.getMyInquiries(userId, pageable));
	}

}
