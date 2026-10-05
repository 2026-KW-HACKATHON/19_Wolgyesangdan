package com.Wolgyesangdan.backend.domain.carbonreport.controller;

import com.Wolgyesangdan.backend.domain.carbonreport.dto.MyImpactResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.service.CarbonReportService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로는 /users/me 아래지만 탄소 성과 집계라 carbonreport 패키지에 둔다.
 */
@RestController
@RequiredArgsConstructor
public class MyImpactController {

	private final CarbonReportService carbonReportService;

	/** 나의 자원순환 기록 (로그인 필요) — 전달·수령 완료 횟수와 탄소 절감량 합계 */
	@GetMapping("/users/me/impact")
	public MyImpactResponse getMyImpact(@AuthenticationPrincipal Long userId) {
		return carbonReportService.getMyImpact(userId);
	}

}
