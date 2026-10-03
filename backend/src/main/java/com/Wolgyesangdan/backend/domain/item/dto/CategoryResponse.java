package com.Wolgyesangdan.backend.domain.item.dto;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;

public record CategoryResponse(CategoryGroup categoryGroup, int carbonReductionKg) {

	public static CategoryResponse from(CategoryCarbonReference reference) {
		return new CategoryResponse(reference.getCategoryGroup(), reference.getCarbonReductionKg());
	}

}
