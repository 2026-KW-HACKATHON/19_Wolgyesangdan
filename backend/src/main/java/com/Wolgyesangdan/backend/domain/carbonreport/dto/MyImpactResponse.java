package com.Wolgyesangdan.backend.domain.carbonreport.dto;

/**
 * 나의 자원순환 기록 — 거래 완료(Reservation.status=COMPLETED)된 건만 센다.
 *
 * @param givenCount 내가 등록한 물품을 전달 완료한 횟수
 * @param receivedCount 내가 신청한 물품을 수령 완료한 횟수
 * @param carbonReductionKg 위 물품들의 예상 탄소 절감량 합계 (kg CO₂e)
 */
public record MyImpactResponse(long givenCount, long receivedCount, long carbonReductionKg) {
}
