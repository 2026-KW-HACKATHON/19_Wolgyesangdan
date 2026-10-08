package com.Wolgyesangdan.backend.domain.reservation.controller;

import com.Wolgyesangdan.backend.domain.reservation.dto.MyTodoResponse;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로는 /users/me 아래지만 배정·예약 기준이라 reservation 패키지에 둔다.
 */
@RestController
@RequiredArgsConstructor
public class MyTodoController {

	private final ReservationService reservationService;

	/** 내가 지금 해야 할 일 (로그인 필요) — 수령 재확인할 신청, 전달할 내 물품 */
	@GetMapping("/users/me/todo")
	public MyTodoResponse getMyTodo(@AuthenticationPrincipal Long userId) {
		return reservationService.getMyTodo(userId);
	}

}
