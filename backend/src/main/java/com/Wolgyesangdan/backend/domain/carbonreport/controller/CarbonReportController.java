package com.Wolgyesangdan.backend.domain.carbonreport.controller;

import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.ReportScope;
import com.Wolgyesangdan.backend.domain.carbonreport.service.CarbonReportService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CarbonReportController {

	private final CarbonReportService carbonReportService;

	/**
	 * 탄소절감 리포트 (비회원도 조회 가능). 로그인 상태면 같은 범위의 내 기여분(me)도 함께 내려준다.
	 * 잘못된 scope 값은 GlobalExceptionHandler가 400 INVALID_INPUT으로 응답한다.
	 */
	@GetMapping("/carbon-report")
	public CarbonReportResponse getReport(@RequestParam(defaultValue = "ALL") ReportScope scope,
			@AuthenticationPrincipal Long userId) {
		return carbonReportService.getReport(scope, userId);
	}

}
