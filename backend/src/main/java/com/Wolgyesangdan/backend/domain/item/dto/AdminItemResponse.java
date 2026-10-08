package com.Wolgyesangdan.backend.domain.item.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;

/**
 * 관리자 물품 관리 표의 한 줄.
 */
public record AdminItemResponse(
		Long id,
		String name,
		String ownerNickname,
		CategoryGroup categoryGroup,
		ItemStatus status,
		LocalDateTime createdAt,
		boolean hidden) {

	public static AdminItemResponse from(Item item) {
		return new AdminItemResponse(
				item.getId(),
				item.getName(),
				item.getOwner().getNickname(),
				item.getCategoryGroup(),
				item.getStatus(),
				item.getCreatedAt(),
				item.isHidden());
	}

}
