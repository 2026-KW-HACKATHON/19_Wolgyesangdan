package com.Wolgyesangdan.backend.domain.item.controller;

import com.Wolgyesangdan.backend.domain.item.dto.AdminItemHiddenRequest;
import com.Wolgyesangdan.backend.domain.item.dto.AdminItemResponse;
import com.Wolgyesangdan.backend.domain.item.service.AdminItemService;
import com.Wolgyesangdan.backend.global.dto.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 물품 관리 (ADMIN만 — SecurityConfig의 /admin/**).
 */
@RestController
@RequestMapping("/admin/items")
@RequiredArgsConstructor
public class AdminItemController {

	private static final int MAX_PAGE_SIZE = 100;

	private final AdminItemService adminItemService;

	/** 물품 목록. hidden을 안 보내면 숨긴 물품까지 전부. page는 0부터, size는 1~100 범위로 보정한다 */
	@GetMapping
	public PageResponse<AdminItemResponse> getItems(
			@RequestParam(required = false) Boolean hidden,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(
				adminItemService.getItems(hidden, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
	}

	/** 숨기기 / 다시 보이기 */
	@PatchMapping("/{itemId}")
	public AdminItemResponse changeHidden(@PathVariable Long itemId, @Valid @RequestBody AdminItemHiddenRequest request) {
		return adminItemService.changeHidden(itemId, request.hidden());
	}

}
