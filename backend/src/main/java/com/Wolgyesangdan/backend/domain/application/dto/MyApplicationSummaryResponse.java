package com.Wolgyesangdan.backend.domain.application.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;

/**
 * 마이페이지 "내가 신청한 물품" 카드 한 장.
 * waitlistRank는 WAITING일 때만 값이 있다 — 다른 상태에서도 엔티티에 예전 순번이 남아있을 수 있어
 * 상태를 보고 null로 가린다.
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
				application.getStatus() == ApplicationStatus.WAITING ? application.getWaitlistRank() : null,
				application.getCreatedAt());
	}

}
