package com.Wolgyesangdan.backend.domain.item.controller;

import com.Wolgyesangdan.backend.domain.item.dto.AdminItemHiddenRequest;
import com.Wolgyesangdan.backend.domain.item.dto.AdminItemResponse;
import com.Wolgyesangdan.backend.domain.item.service.AdminItemService;
import com.Wolgyesangdan.backend.global.dto.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

	private final AdminItemService adminItemService;

	/** 물품 목록. hidden을 안 보내면 숨긴 물품까지 전부. page는 0부터, size 기본 20 */
	@GetMapping
	public PageResponse<AdminItemResponse> getItems(
			@RequestParam(required = false) Boolean hidden,
			@PageableDefault(size = 20) Pageable pageable) {
		return PageResponse.from(adminItemService.getItems(hidden, pageable));
	}

	/** 숨기기 / 다시 보이기 */
	@PatchMapping("/{itemId}")
	public AdminItemResponse changeHidden(@PathVariable Long itemId, @Valid @RequestBody AdminItemHiddenRequest request) {
		return adminItemService.changeHidden(itemId, request.hidden());
	}

	/** 신청 조기 마감 — 지금 마감하고 바로 1순위에게 배정한다 (#274) */
	@PostMapping("/{itemId}/close-applications")
	public AdminItemResponse closeApplications(@PathVariable Long itemId) {
		return adminItemService.closeApplications(itemId);
	}

}
