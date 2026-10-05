package com.Wolgyesangdan.backend.domain.reservation.controller;

import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmResponse;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	/** 수령 재확인 (로그인 필요, 배정된 신청자 본인만) */
	@PatchMapping("/{reservationId}/reconfirm")
	public ReconfirmResponse reconfirm(@AuthenticationPrincipal Long userId, @PathVariable Long reservationId) {
		return reservationService.reconfirm(userId, reservationId);
	}

}
