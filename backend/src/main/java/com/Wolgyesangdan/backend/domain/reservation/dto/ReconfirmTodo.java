package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

/** 신청자의 할 일 — 배정된 물품을 기한 안에 수령 재확인해야 한다 */
public record ReconfirmTodo(
		Long applicationId,
		Long reservationId,
		Long itemId,
		String itemName,
		LocalDateTime reconfirmationDeadline) {
}
