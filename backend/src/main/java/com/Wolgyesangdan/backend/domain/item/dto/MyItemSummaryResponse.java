package com.Wolgyesangdan.backend.domain.item.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;

/**
 * 마이페이지 "내가 등록한 물품" 카드 한 장.
 * applicationId(배정된 신청)와 scheduledAt은 배정 확정 뒤(ASSIGNED·COMPLETED)에만 값이 있다.
 * 등록자는 applicationId로 예약 상세를 조회해 상대 연락처를 보고 전달 완료를 처리한다.
 */
public record MyItemSummaryResponse(
		Long id,
		String name,
		String thumbnailImageUrl,
		ItemStatus status,
		int applicantCount,
		Long applicationId,
		LocalDateTime scheduledAt) {

	public static MyItemSummaryResponse of(Item item, String thumbnailImageUrl, Long applicationId,
			LocalDateTime scheduledAt) {
		return new MyItemSummaryResponse(
				item.getId(),
				item.getName(),
				thumbnailImageUrl,
				item.getStatus(),
				item.getApplicantCount(),
				applicationId,
				scheduledAt);
	}

}
