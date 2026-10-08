package com.Wolgyesangdan.backend.domain.application.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;

/**
 * 마이페이지 "내가 신청한 물품" 카드 한 장.
 * waitlistRank는 배정(SELECTED) 이후에도 그대로 보여준다 (요구사항 APPL-07, 2026-09-26 결정) —
 * 취소(#90)만 null로 지우고, 배정·노쇼 승계(#92·#98)는 값을 남겨둔다.
 * 단, 물품이 아직 배정 전이면 순번을 알려주지 않는다(null) — 순번은 배정이 끝난 뒤에만 공개한다 (#265).
 * itemStatus는 물품의 지금 상태 — 대기 중(WAITING)인 신청인데 물품이 COMPLETED면,
 * 다른 신청자와 거래가 끝나 배정받지 못한 신청이다 (#265).
 */
public record MyApplicationSummaryResponse(
		Long id,
		Long itemId,
		String itemName,
		String itemThumbnailImageUrl,
		ApplicationStatus status,
		Integer waitlistRank,
		ItemStatus itemStatus,
		LocalDateTime appliedAt) {

	public static MyApplicationSummaryResponse of(Application application, String itemThumbnailImageUrl) {
		return new MyApplicationSummaryResponse(
				application.getId(),
				application.getItem().getId(),
				application.getItem().getName(),
				itemThumbnailImageUrl,
				application.getStatus(),
				application.visibleWaitlistRank(),
				application.getItem().getStatus(),
				application.getCreatedAt());
	}

}
