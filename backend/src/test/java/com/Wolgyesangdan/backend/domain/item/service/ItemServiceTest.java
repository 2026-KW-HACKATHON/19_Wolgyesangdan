package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ItemServiceTest {

	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository =
			Mockito.mock(CategoryCarbonReferenceRepository.class);
	private final ItemService itemService = new ItemService(categoryCarbonReferenceRepository);

	@Test
	void 카테고리는_DB_순서와_상관없이_대분류_선언_순서로_내려준다() {
		given(categoryCarbonReferenceRepository.findAll()).willReturn(List.of(
				reference(CategoryGroup.ETC, 8),
				reference(CategoryGroup.KITCHEN, 10),
				reference(CategoryGroup.FURNITURE, 30),
				reference(CategoryGroup.LIVING, 15),
				reference(CategoryGroup.APPLIANCE, 24)));

		assertThat(itemService.getCategories()).containsExactly(
				new CategoryResponse(CategoryGroup.FURNITURE, 30),
				new CategoryResponse(CategoryGroup.APPLIANCE, 24),
				new CategoryResponse(CategoryGroup.KITCHEN, 10),
				new CategoryResponse(CategoryGroup.LIVING, 15),
				new CategoryResponse(CategoryGroup.ETC, 8));
	}

	private static CategoryCarbonReference reference(CategoryGroup categoryGroup, int carbonReductionKg) {
		return CategoryCarbonReference.builder()
				.categoryGroup(categoryGroup)
				.carbonReductionKg(carbonReductionKg)
				.build();
	}

}
