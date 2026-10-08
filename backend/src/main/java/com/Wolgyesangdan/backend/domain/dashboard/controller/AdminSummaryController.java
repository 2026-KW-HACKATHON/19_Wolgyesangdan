package com.Wolgyesangdan.backend.domain.dashboard.controller;

import com.Wolgyesangdan.backend.domain.dashboard.dto.AdminSummaryResponse;
import com.Wolgyesangdan.backend.domain.dashboard.service.AdminSummaryService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드 요약 (ADMIN만 — SecurityConfig의 /admin/**).
 */
@RestController
@RequestMapping("/admin/summary")
@RequiredArgsConstructor
public class AdminSummaryController {

	private final AdminSummaryService adminSummaryService;

	/** 처리할 일 건수와 현재 캠페인. 캠페인이 없으면 currentCampaign이 null */
	@GetMapping
	public AdminSummaryResponse getSummary() {
		return adminSummaryService.getSummary();
	}

}
