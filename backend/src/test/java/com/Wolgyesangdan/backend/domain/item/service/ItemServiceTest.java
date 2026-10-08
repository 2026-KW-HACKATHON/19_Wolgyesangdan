package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.ItemType;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemImageRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemTradeMethodRepository;
import com.Wolgyesangdan.backend.domain.application.repository.ApplicationRepository;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ItemServiceTest {

	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository =
			Mockito.mock(CategoryCarbonReferenceRepository.class);
	private final ItemService itemService = new ItemService(Mockito.mock(ItemRepository.class),
			Mockito.mock(ItemImageRepository.class), Mockito.mock(ItemTradeMethodRepository.class),
			categoryCarbonReferenceRepository, Mockito.mock(ReservationRepository.class),
			Mockito.mock(UserRepository.class), Mockito.mock(CampaignRepository.class),
			Mockito.mock(ApplicationRepository.class));

	@Test
	void 카테고리는_DB_순서와_상관없이_대분류_선언_순서로_내려준다() {
		given(categoryCarbonReferenceRepository.findAll()).willReturn(List.of(
				reference(CategoryGroup.ETC, 8),
				reference(CategoryGroup.KITCHEN, 10),
				reference(CategoryGroup.FURNITURE, 30),
				reference(CategoryGroup.LIVING, 15),
				reference(CategoryGroup.APPLIANCE, 24)));

		assertThat(itemService.getCategories()).extracting(CategoryResponse::categoryGroup, CategoryResponse::carbonReductionKg)
				.containsExactly(
						tuple(CategoryGroup.FURNITURE, 30),
						tuple(CategoryGroup.APPLIANCE, 24),
						tuple(CategoryGroup.KITCHEN, 10),
						tuple(CategoryGroup.LIVING, 15),
						tuple(CategoryGroup.ETC, 8));
	}

	@Test
	void 대분류마다_속한_품목을_선언_순서대로_함께_내려준다() {
		given(categoryCarbonReferenceRepository.findAll()).willReturn(List.of(
				reference(CategoryGroup.APPLIANCE, 70), reference(CategoryGroup.ETC, 8)));

		List<CategoryResponse> categories = itemService.getCategories();

		CategoryResponse appliance = categories.getFirst();
		assertThat(appliance.itemTypes()).extracting(CategoryResponse.ItemTypeResponse::itemType)
				.containsExactlyElementsOf(ItemType.of(CategoryGroup.APPLIANCE));
		CategoryResponse.ItemTypeResponse refrigerator = appliance.itemTypes().getFirst();
		assertThat(refrigerator.itemType()).isEqualTo(ItemType.REFRIGERATOR);
		assertThat(refrigerator.label()).isEqualTo("냉장고");
		assertThat(refrigerator.carbonReductionKg()).isEqualTo(240);
		assertThat(refrigerator.basis()).contains("UK DESNZ 2024");
		assertThat(categories.get(1).itemTypes()).extracting(CategoryResponse.ItemTypeResponse::itemType)
				.containsExactly(ItemType.BICYCLE);
	}

	private static CategoryCarbonReference reference(CategoryGroup categoryGroup, int carbonReductionKg) {
		return CategoryCarbonReference.builder()
				.categoryGroup(categoryGroup)
				.carbonReductionKg(carbonReductionKg)
				.build();
	}

}
