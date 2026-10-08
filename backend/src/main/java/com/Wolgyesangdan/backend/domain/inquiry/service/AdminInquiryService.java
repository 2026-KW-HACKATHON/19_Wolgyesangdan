package com.Wolgyesangdan.backend.domain.inquiry.service;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquiryDetailResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquirySummaryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryAnswerRequest;
import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;
import com.Wolgyesangdan.backend.domain.inquiry.exception.InquiryErrorCode;
import com.Wolgyesangdan.backend.domain.inquiry.repository.InquiryRepository;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 문의 관리 (#211) — 목록·상세·답변. 답변 수정·삭제는 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

	private final InquiryRepository inquiryRepository;
	private final UserRepository userRepository;

	/**
	 * 문의 목록 — 답변 대기가 먼저, 그 안에서는 최근에 들어온 순 (요청의 sort는 쓰지 않는다). 한 페이지는 최대 100개.
	 *
	 * @param status null이면 전체
	 */
	public Page<AdminInquirySummaryResponse> getInquiries(InquiryStatus status, Pageable pageable) {
		return inquiryRepository.findAllForAdmin(status,
						PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), InquiryService.MAX_PAGE_SIZE)))
				.map(AdminInquirySummaryResponse::from);
	}

	public AdminInquiryDetailResponse getInquiry(Long inquiryId) {
		return AdminInquiryDetailResponse.from(findInquiry(inquiryId));
	}

	/** 답변 등록. 이미 답변한 문의면 409 INQUIRY_ALREADY_ANSWERED */
	@Transactional
	public AdminInquiryDetailResponse answer(Long adminId, Long inquiryId, InquiryAnswerRequest request) {
		Inquiry inquiry = findInquiry(inquiryId);
		if (inquiry.isAnswered()) {
			throw new BusinessException(InquiryErrorCode.INQUIRY_ALREADY_ANSWERED);
		}
		User admin = userRepository.findById(adminId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_USER_NOT_FOUND));
		inquiry.answer(request.answer().strip(), admin, LocalDateTime.now());
		return AdminInquiryDetailResponse.from(inquiry);
	}

	private Inquiry findInquiry(Long inquiryId) {
		return inquiryRepository.findWithAuthorById(inquiryId)
				.orElseThrow(() -> new BusinessException(InquiryErrorCode.INQUIRY_NOT_FOUND));
	}

}
