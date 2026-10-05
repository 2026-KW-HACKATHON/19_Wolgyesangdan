package com.Wolgyesangdan.backend.domain.application.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;

/**
 * 마이페이지 "내가 신청한 물품" 카드 한 장.
 * waitlistRank는 배정(SELECTED) 이후에도 그대로 보여준다 (요구사항 APPL-07, 2026-09-26 결정) —
 * 취소(#90)만 null로 지우고, 배정·노쇼 승계(#92·#98)는 값을 남겨두므로 엔티티 값을 그대로 내려준다.
 */
public record MyApplicationSummaryResponse(
		Long id,
		Long itemId,
		String itemName,
		String itemThumbnailImageUrl,
		ApplicationStatus status,
		Integer waitlistRank,
		LocalDateTime appliedAt) {

	public static MyApplicationSummaryResponse of(Application application, String itemThumbnailImageUrl) {
		return new MyApplicationSummaryResponse(
				application.getId(),
				application.getItem().getId(),
				application.getItem().getName(),
				itemThumbnailImageUrl,
				application.getStatus(),
				application.getWaitlistRank(),
				application.getCreatedAt());
	}

}
