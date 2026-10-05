package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemImageUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ItemImageUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemImageRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemTradeMethodRepository;

import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.storage.ImageStorage;
import com.Wolgyesangdan.backend.global.storage.PresignedUpload;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ItemServiceTest {

	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository =
			Mockito.mock(CategoryCarbonReferenceRepository.class);
	private final ImageStorage imageStorage = Mockito.mock(ImageStorage.class);
	private final ItemService itemService = new ItemService(Mockito.mock(ItemRepository.class),
			Mockito.mock(ItemImageRepository.class), Mockito.mock(ItemTradeMethodRepository.class),
			categoryCarbonReferenceRepository, imageStorage);

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

	@Test
	void 사진_업로드_URL은_날짜별_경로에_UUID_파일명으로_발급한다() {
		given(imageStorage.isAvailable()).willReturn(true);
		given(imageStorage.presignPut(anyString(), eq("image/png")))
				.willAnswer(invocation -> new PresignedUpload("https://upload/" + invocation.getArgument(0),
						"https://file/" + invocation.getArgument(0)));

		ItemImageUploadUrlResponse response =
				itemService.issueImageUploadUrl(new ItemImageUploadUrlRequest("내 사진 1.png", "image/png"));

		assertThat(response.imageUrl())
				.matches("https://file/items/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}\\.png");
		assertThat(response.uploadUrl()).startsWith("https://upload/items/");
	}

	@Test
	void S3가_설정되지_않았으면_ITEM_IMAGE_UPLOAD_UNAVAILABLE() {
		given(imageStorage.isAvailable()).willReturn(false);

		assertThatThrownBy(() -> itemService.issueImageUploadUrl(new ItemImageUploadUrlRequest("a.jpg", "image/jpeg")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_IMAGE_UPLOAD_UNAVAILABLE);
	}

	private static CategoryCarbonReference reference(CategoryGroup categoryGroup, int carbonReductionKg) {
		return CategoryCarbonReference.builder()
				.categoryGroup(categoryGroup)
				.carbonReductionKg(carbonReductionKg)
				.build();
	}

}
