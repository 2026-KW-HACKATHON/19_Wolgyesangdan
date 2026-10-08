package com.Wolgyesangdan.backend.domain.inquiry.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;

/**
 * 관리자 문의 상세 — 목록 항목 + 본문·답변.
 */
public record AdminInquiryDetailResponse(
		Long id,
		InquiryCategory category,
		String title,
		String nickname,
		LocalDateTime createdAt,
		InquiryStatus status,
		String content,
		String answer,
		LocalDateTime answeredAt) {

	public static AdminInquiryDetailResponse from(Inquiry inquiry) {
		return new AdminInquiryDetailResponse(
				inquiry.getId(),
				inquiry.getCategory(),
				inquiry.getTitle(),
				inquiry.getAuthor().getNickname(),
				inquiry.getCreatedAt(),
				inquiry.getStatus(),
				inquiry.getContent(),
				inquiry.getAnswer(),
				inquiry.getAnsweredAt());
	}

}
