package com.Wolgyesangdan.backend.domain.item.dto;

/**
 * 거래 완료된 물품 집계 — 물품 수와 예상 탄소 절감량 합계 (ReservationRepository.summarizeCompletedInCampaign).
 */
public record CompletedItemSummary(long count, long carbonReductionKg) {
}
