package com.Wolgyesangdan.backend.domain.item.dto;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.ItemType;

/**
 * 카테고리 대분류와 그 아래 품목 (#284).
 *
 * @param carbonReductionKg 품목을 고르지 않았을 때(목록에 없는 물건) 쓰는 대분류 값
 * @param basis             대분류 값의 근거
 * @param itemTypes         대분류에 속한 품목 (등록 화면 표시 순서)
 */
public record CategoryResponse(CategoryGroup categoryGroup, int carbonReductionKg, String basis,
		List<ItemTypeResponse> itemTypes) {

	public static CategoryResponse from(CategoryCarbonReference reference) {
		return new CategoryResponse(reference.getCategoryGroup(), reference.getCarbonReductionKg(),
				reference.getSource(), ItemType.of(reference.getCategoryGroup()).stream()
						.map(ItemTypeResponse::from)
						.toList());
	}

	/**
	 * @param itemType 등록 요청에 보낼 값 (예: REFRIGERATOR)
	 * @param label    화면 표시 이름 (예: 냉장고)
	 */
	public record ItemTypeResponse(ItemType itemType, String label, int carbonReductionKg, String basis) {

		static ItemTypeResponse from(ItemType itemType) {
			return new ItemTypeResponse(itemType, itemType.getLabel(), itemType.getCarbonReductionKg(),
					itemType.getBasis());
		}
	}

}
