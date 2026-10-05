package com.Wolgyesangdan.backend.domain.item.controller;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.service.ItemService;

import com.Wolgyesangdan.backend.global.dto.PageResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
	 * 물품 목록 (비회원 허용). page는 0부터, size는 1~100 범위로 보정한다.
	 */
	@GetMapping
	public PageResponse<ItemSummaryResponse> getItems(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(itemService.getItems(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
	}

	@GetMapping("/categories")
	public List<CategoryResponse> getCategories() {
		return itemService.getCategories();
	}

}
