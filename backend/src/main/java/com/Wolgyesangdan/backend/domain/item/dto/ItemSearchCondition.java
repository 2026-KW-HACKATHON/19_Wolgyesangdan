package com.Wolgyesangdan.backend.domain.item.dto;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;

/**
 * 물품 목록 검색 조건. null인 조건은 적용하지 않는다.
 */
public record ItemSearchCondition(String keyword, CategoryGroup categoryGroup, TradeMethod tradeMethod, ItemSort sort) {

	public ItemSearchCondition {
		keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
		sort = sort == null ? ItemSort.LATEST : sort;
	}

	public static ItemSearchCondition none() {
		return new ItemSearchCondition(null, null, null, ItemSort.LATEST);
	}

}
