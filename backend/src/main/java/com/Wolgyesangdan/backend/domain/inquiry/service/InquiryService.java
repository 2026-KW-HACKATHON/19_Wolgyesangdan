package com.Wolgyesangdan.backend.domain.inquiry.service;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryCreateRequest;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.repository.InquiryRepository;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 문의 (#211) — 작성과 내 문의 조회. 알림은 없고 회원이 앱에서 답변을 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

	static final int MAX_PAGE_SIZE = 100;
	private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	private final InquiryRepository inquiryRepository;
	private final UserRepository userRepository;

	@Transactional
	public InquiryResponse createInquiry(Long userId, InquiryCreateRequest request) {
		User author = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_USER_NOT_FOUND));
		Inquiry inquiry = inquiryRepository.save(Inquiry.builder()
				.author(author)
				.category(request.category())
				.title(request.title().strip())
				.content(request.content().strip())
				.build());
		return InquiryResponse.from(inquiry);
	}

	/** 내 문의 — 최근에 쓴 순 (요청의 sort는 쓰지 않는다). 한 페이지는 최대 100개 */
	public Page<InquiryResponse> getMyInquiries(Long userId, Pageable pageable) {
		return inquiryRepository.findByAuthorId(userId,
						PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), LATEST))
				.map(InquiryResponse::from);
	}

}
