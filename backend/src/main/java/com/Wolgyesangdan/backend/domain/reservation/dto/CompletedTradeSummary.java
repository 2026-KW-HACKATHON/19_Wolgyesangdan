package com.Wolgyesangdan.backend.domain.reservation.dto;

/** 거래 완료된 예약 수와 그 물품들의 예상 탄소 절감량 합계 (집계 쿼리 결과) */
public record CompletedTradeSummary(long count, long carbonReductionKg) {
}
