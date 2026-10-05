package com.Wolgyesangdan.backend.domain.carbonreport.service;

import com.Wolgyesangdan.backend.domain.carbonreport.dto.MyImpactResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.TradeCounts;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탄소 절감 성과 집계. 자기 엔티티 없이 거래 완료된 예약·물품을 모아 계산한다.
 * ERD 결정대로 캐시 필드 없이 조회할 때마다 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarbonReportService {

	private final ReservationRepository reservationRepository;

	public MyImpactResponse getMyImpact(Long userId) {
		TradeCounts counts = reservationRepository.summarizeTradesByUserId(userId);
		return new MyImpactResponse(counts.givenCount(), counts.receivedCount(), counts.carbonReductionKg());
	}

}
