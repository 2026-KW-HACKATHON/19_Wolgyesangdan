package com.Wolgyesangdan.backend.domain.item.dto;

/**
 * 물품 목록 정렬 기준. 같은 순위끼리는 항상 최신 등록순으로 정렬한다.
 */
public enum ItemSort {
	/** 최신 등록순 (기본) */
	LATEST,
	/** 예상 탄소 절감량 많은 순 */
	CARBON,
	/** 물품 상태 좋은 순 */
	CONDITION
}
