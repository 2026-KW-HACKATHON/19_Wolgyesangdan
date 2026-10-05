package com.Wolgyesangdan.backend.domain.item.controller;

import com.Wolgyesangdan.backend.domain.item.dto.MyItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.service.ItemService;
import com.Wolgyesangdan.backend.global.dto.PageResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로는 /users/me 아래지만 물품 도메인 데이터라 item 패키지에 둔다.
 */
@RestController
@RequiredArgsConstructor
public class MyItemController {

	private static final int MAX_PAGE_SIZE = 100;

	private final ItemService itemService;

	/** 내가 등록한 물품 (로그인 필요). 상태와 관계없이 전부, 최근 등록순 */
	@GetMapping("/users/me/items")
	public PageResponse<MyItemSummaryResponse> getMyItems(@AuthenticationPrincipal Long userId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(
				itemService.getMyItems(userId, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
	}

}
