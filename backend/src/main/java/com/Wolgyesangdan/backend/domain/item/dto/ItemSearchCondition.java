package com.Wolgyesangdan.backend.domain.item.dto;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;

/**
 * 물품 목록 검색 조건. null인 조건은 적용하지 않는다. availability가 없으면 ACTIVE(거래가 끝난 물건 숨김, #190).
 */
public record ItemSearchCondition(String keyword, CategoryGroup categoryGroup, TradeMethod tradeMethod, ItemSort sort,
		ItemAvailability availability) {

	public ItemSearchCondition {
		keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
		sort = sort == null ? ItemSort.LATEST : sort;
		availability = availability == null ? ItemAvailability.ACTIVE : availability;
	}

	/** 상태 범위는 기본(ACTIVE) */
	public ItemSearchCondition(String keyword, CategoryGroup categoryGroup, TradeMethod tradeMethod, ItemSort sort) {
		this(keyword, categoryGroup, tradeMethod, sort, null);
	}

	public static ItemSearchCondition none() {
		return new ItemSearchCondition(null, null, null, ItemSort.LATEST, ItemAvailability.ACTIVE);
	}

}
