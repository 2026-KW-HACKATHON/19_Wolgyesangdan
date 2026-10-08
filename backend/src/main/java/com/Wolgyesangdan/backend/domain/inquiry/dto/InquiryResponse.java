package com.Wolgyesangdan.backend.domain.inquiry.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;

/**
 * 회원에게 보여주는 내 문의 — 답변까지 함께 내려준다 (답변 전이면 answer·answeredAt은 null).
 */
public record InquiryResponse(
		Long id,
		InquiryCategory category,
		String title,
		String content,
		InquiryStatus status,
		String answer,
		LocalDateTime answeredAt,
		LocalDateTime createdAt) {

	public static InquiryResponse from(Inquiry inquiry) {
		return new InquiryResponse(
				inquiry.getId(),
				inquiry.getCategory(),
				inquiry.getTitle(),
				inquiry.getContent(),
				inquiry.getStatus(),
				inquiry.getAnswer(),
				inquiry.getAnsweredAt(),
				inquiry.getCreatedAt());
	}

}
