package com.Wolgyesangdan.backend.domain.reservation.dto;

/**
 * 한 사용자의 거래 완료 집계 (ReservationRepository 집계 쿼리 결과).
 *
 * @param givenCount 등록자로서 전달 완료한 수
 * @param receivedCount 신청자로서 수령 완료한 수
 * @param carbonReductionKg 위 물품들의 예상 탄소 절감량 합계
 */
public record TradeCounts(long givenCount, long receivedCount, long carbonReductionKg) {
}
