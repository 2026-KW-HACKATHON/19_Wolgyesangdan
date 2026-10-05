package com.Wolgyesangdan.backend.domain.item.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemImageUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ItemImageUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSearchCondition;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemImageRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemSpecifications;
import com.Wolgyesangdan.backend.domain.item.repository.ItemTradeMethodRepository;

import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.storage.ImageStorage;
import com.Wolgyesangdan.backend.global.storage.PresignedUpload;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

	/** 목록에 보여주는 상태 — 취소(CANCELED)와 신청 기간 전(REGISTERED)은 제외 */
	static final List<ItemStatus> LISTED_STATUSES =
			List.of(ItemStatus.OPEN, ItemStatus.CLOSED, ItemStatus.ASSIGNED, ItemStatus.COMPLETED);

	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;
	private final ItemTradeMethodRepository itemTradeMethodRepository;
	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository;
	private final ImageStorage imageStorage;

	private static final DateTimeFormatter IMAGE_DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp",
			"image/heic", "heic",
			"image/heif", "heif");

	/**
	 * 물품 목록. 검색·필터·정렬은 ItemSpecifications에서 처리한다.
	 * 대표 사진과 거래 방식은 물품마다 따로 조회하지 않고 페이지에 담긴 물품 id로 한 번씩만 조회해서 붙인다 (N+1 방지).
	 */
	public Page<ItemSummaryResponse> getItems(ItemSearchCondition condition, int page, int size) {
		Page<Item> items = itemRepository.findAll(ItemSpecifications.search(LISTED_STATUSES, condition),
				PageRequest.of(page, size));
		List<Long> itemIds = items.map(Item::getId).getContent();
		if (itemIds.isEmpty()) {
			return items.map(item -> ItemSummaryResponse.of(item, null, List.of()));
		}

		Map<Long, String> thumbnails = itemImageRepository.findByItemIdIn(itemIds).stream()
				.collect(Collectors.groupingBy(image -> image.getItem().getId(),
						Collectors.collectingAndThen(
								Collectors.minBy(Comparator.comparingInt(ItemImage::getDisplayOrder)),
								image -> image.map(ItemImage::getImageUrl).orElse(null))));
		Map<Long, List<TradeMethod>> tradeMethods = itemTradeMethodRepository.findByItemIdIn(itemIds).stream()
				.collect(Collectors.groupingBy(tradeMethod -> tradeMethod.getItem().getId(),
						Collectors.mapping(ItemTradeMethod::getTradeMethod,
								Collectors.collectingAndThen(Collectors.toList(),
										list -> list.stream().sorted().toList()))));

		return items.map(item -> ItemSummaryResponse.of(item,
				thumbnails.get(item.getId()),
				tradeMethods.getOrDefault(item.getId(), List.of())));
	}

	/**
	 * 카테고리 대분류별 예상 탄소 절감량. 순서는 CategoryGroup 선언 순서(가구·가전·주방·생활·기타)로 고정 —
	 * 프론트 필터 칩/등록 폼이 이 순서 그대로 그린다.
	 */
	public List<CategoryResponse> getCategories() {
		return categoryCarbonReferenceRepository.findAll().stream()
				.sorted(Comparator.comparing(CategoryCarbonReference::getCategoryGroup))
				.map(CategoryResponse::from)
				.toList();
	}

	/**
	 * 물품 사진 업로드용 presigned URL 발급. 물품이 생기기 전(등록 폼 작성 중)에 호출되므로 itemId 없이 발급한다.
	 * 경로: items/{yyyy}/{MM}/{dd}/{uuid}.{확장자}
	 */
	public ItemImageUploadUrlResponse issueImageUploadUrl(ItemImageUploadUrlRequest request) {
		if (!imageStorage.isAvailable()) {
			throw new BusinessException(ItemErrorCode.ITEM_IMAGE_UPLOAD_UNAVAILABLE);
		}
		String key = "items/" + LocalDate.now().format(IMAGE_DATE_PATH) + "/" + UUID.randomUUID() + "."
				+ IMAGE_EXTENSIONS.get(request.contentType());
		PresignedUpload upload = imageStorage.presignPut(key, request.contentType());
		return new ItemImageUploadUrlResponse(upload.uploadUrl(), upload.fileUrl());
	}

}
