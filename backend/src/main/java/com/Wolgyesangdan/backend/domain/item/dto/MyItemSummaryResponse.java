package com.Wolgyesangdan.backend.domain.item.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;

/**
 * 마이페이지 "내가 등록한 물품" 카드 한 장.
 * scheduledAt은 배정 확정 뒤(ASSIGNED·COMPLETED)에만 값이 있다.
 */
public record MyItemSummaryResponse(
		Long id,
		String name,
		String thumbnailImageUrl,
		ItemStatus status,
		int applicantCount,
		LocalDateTime scheduledAt) {

	public static MyItemSummaryResponse of(Item item, String thumbnailImageUrl, LocalDateTime scheduledAt) {
		return new MyItemSummaryResponse(
				item.getId(),
				item.getName(),
				thumbnailImageUrl,
				item.getStatus(),
				item.getApplicantCount(),
				scheduledAt);
	}

}
