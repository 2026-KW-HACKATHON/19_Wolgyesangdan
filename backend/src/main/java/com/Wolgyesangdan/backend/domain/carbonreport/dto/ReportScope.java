package com.Wolgyesangdan.backend.domain.carbonreport.dto;

/** 탄소절감 리포트 집계 범위 */
public enum ReportScope {
	/** 월계1동 전체 누적 */
	ALL,
	/** 진행 중(없으면 예정) 캠페인의 물품만 */
	CAMPAIGN
}
