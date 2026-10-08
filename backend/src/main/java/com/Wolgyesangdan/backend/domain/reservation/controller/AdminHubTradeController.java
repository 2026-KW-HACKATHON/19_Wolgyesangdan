package com.Wolgyesangdan.backend.domain.reservation.controller;

import com.Wolgyesangdan.backend.domain.reservation.dto.AdminHubTradeResponse;
import com.Wolgyesangdan.backend.domain.reservation.service.AdminHubTradeService;
import com.Wolgyesangdan.backend.global.dto.PageResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 거점 거래 처리 (ADMIN만 — SecurityConfig의 /admin/**).
 */
@RestController
@RequestMapping("/admin/hub-trades")
@RequiredArgsConstructor
public class AdminHubTradeController {

	private final AdminHubTradeService adminHubTradeService;

	/** 거점 거래 목록 — 최근 배정순. done=false면 진행 중, true면 끝난 거래, 안 보내면 전부. page는 0부터, size 기본 20 */
	@GetMapping
	public PageResponse<AdminHubTradeResponse> getHubTrades(
			@RequestParam(required = false) Boolean done,
			@PageableDefault(size = 20) Pageable pageable) {
		return PageResponse.from(adminHubTradeService.getHubTrades(done, pageable));
	}

	/** 거점 입고 처리 */
	@PostMapping("/{reservationId}/receive")
	public AdminHubTradeResponse receive(@PathVariable Long reservationId) {
		return adminHubTradeService.receive(reservationId);
	}

	/** 수령 완료 — 거래 완료. 입고된 뒤에만 */
	@PostMapping("/{reservationId}/complete")
	public AdminHubTradeResponse complete(@PathVariable Long reservationId) {
		return adminHubTradeService.complete(reservationId);
	}

	/** 미수령 처리 — 다음 대기자에게 넘긴다. 입고된 뒤에만 */
	@PostMapping("/{reservationId}/no-show")
	public AdminHubTradeResponse markNoShow(@PathVariable Long reservationId) {
		return adminHubTradeService.markNoShow(reservationId);
	}

}
