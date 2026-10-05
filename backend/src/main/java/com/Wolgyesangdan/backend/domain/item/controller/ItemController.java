package com.Wolgyesangdan.backend.domain.item.controller;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemImageUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ItemImageUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSearchCondition;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSort;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.service.ItemService;

import com.Wolgyesangdan.backend.global.dto.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

	private static final int MAX_PAGE_SIZE = 100;

	private final ItemService itemService;

	/**
	 * 물품 목록 (비회원 허용). 모든 검색 조건은 선택이다.
	 * categoryGroup은 한글 표시값(가구 등)으로 받는다. page는 0부터, size는 1~100 범위로 보정한다.
	 */
	@GetMapping
	public PageResponse<ItemSummaryResponse> getItems(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) CategoryGroup categoryGroup,
			@RequestParam(required = false) TradeMethod tradeMethod,
			@RequestParam(defaultValue = "LATEST") ItemSort sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		ItemSearchCondition condition = new ItemSearchCondition(keyword, categoryGroup, tradeMethod, sort);
		return PageResponse.from(
				itemService.getItems(condition, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
	}

	@GetMapping("/categories")
	public List<CategoryResponse> getCategories() {
		return itemService.getCategories();
	}

	/**
	 * 물품 사진 업로드 URL 발급 (로그인 필요). 브라우저는 uploadUrl로 사진을 PUT 업로드하고,
	 * imageUrl을 물품 등록 요청에 담는다.
	 */
	@PostMapping("/images/upload-url")
	public ItemImageUploadUrlResponse issueImageUploadUrl(@Valid @RequestBody ItemImageUploadUrlRequest request) {
		return itemService.issueImageUploadUrl(request);
	}

}
