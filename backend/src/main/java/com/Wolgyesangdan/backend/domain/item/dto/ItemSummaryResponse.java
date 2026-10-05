package com.Wolgyesangdan.backend.domain.item.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;

/**
 * 물품 목록 카드 한 장.
 */
public record ItemSummaryResponse(
		Long id,
		String name,
		String category,
		CategoryGroup categoryGroup,
		String conditionGrade,
		String thumbnailImageUrl,
		int estimatedCarbonReduction,
		List<TradeMethod> tradeMethods,
		ItemStatus status,
		int applicantCount,
		int maxApplicants,
		LocalDateTime applicationDeadline) {

	public static ItemSummaryResponse of(Item item, String thumbnailImageUrl, List<TradeMethod> tradeMethods) {
		return new ItemSummaryResponse(
				item.getId(),
				item.getName(),
				item.getCategory(),
				item.getCategoryGroup(),
				item.getConditionGrade(),
				thumbnailImageUrl,
				item.getEstimatedCarbonReduction(),
				tradeMethods,
				item.getStatus(),
				item.getApplicantCount(),
				Item.MAX_APPLICANTS,
				item.getApplicationDeadline());
	}

}
