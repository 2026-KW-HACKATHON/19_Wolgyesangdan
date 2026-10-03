package com.Wolgyesangdan.backend.domain.item.dto;

/**
 * 거래완료(COMPLETED)된 물품 집계 — 물품 수와 예상 탄소 절감량 합계.
 */
public record CompletedItemSummary(long count, long carbonReductionKg) {
}
