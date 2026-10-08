package com.Wolgyesangdan.backend.domain.inquiry.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;

/**
 * 관리자 문의 목록의 한 줄.
 *
 * @param nickname 작성자 닉네임
 */
public record AdminInquirySummaryResponse(
		Long id,
		InquiryCategory category,
		String title,
		String nickname,
		LocalDateTime createdAt,
		InquiryStatus status) {

	public static AdminInquirySummaryResponse from(Inquiry inquiry) {
		return new AdminInquirySummaryResponse(
				inquiry.getId(),
				inquiry.getCategory(),
				inquiry.getTitle(),
				inquiry.getAuthor().getNickname(),
				inquiry.getCreatedAt(),
				inquiry.getStatus());
	}

}
