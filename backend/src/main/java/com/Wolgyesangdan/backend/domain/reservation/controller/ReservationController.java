package com.Wolgyesangdan.backend.domain.reservation.controller;

import com.Wolgyesangdan.backend.domain.reservation.dto.CompleteResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReservationDetailResponse;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

	/** 수령 재확인 (로그인 필요, 배정된 신청자 본인만) */
	@PatchMapping("/reservations/{reservationId}/reconfirm")
	public ReconfirmResponse reconfirm(@AuthenticationPrincipal Long userId, @PathVariable Long reservationId) {
		return reservationService.reconfirm(userId, reservationId);
	}

	/** 직거래 전달 완료 (로그인 필요, 물품 등록자만, 신청자가 재확인한 예약만) */
	@PatchMapping("/reservations/{reservationId}/complete")
	public CompleteResponse complete(@AuthenticationPrincipal Long userId, @PathVariable Long reservationId) {
		return reservationService.complete(userId, reservationId);
	}

}
