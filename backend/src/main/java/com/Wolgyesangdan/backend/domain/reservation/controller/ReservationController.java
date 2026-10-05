package com.Wolgyesangdan.backend.domain.reservation.controller;

import com.Wolgyesangdan.backend.domain.reservation.dto.ReservationDetailResponse;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	/** 예약 상세 — 신청자 본인 또는 물품 등록자만 */
	@GetMapping("/applications/{applicationId}/reservation")
	public ReservationDetailResponse getReservation(@AuthenticationPrincipal Long userId,
			@PathVariable Long applicationId) {
		return reservationService.getReservation(userId, applicationId);
	}

}
