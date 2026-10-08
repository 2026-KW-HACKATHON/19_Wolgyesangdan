package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;

/** 등록자의 할 일 — 내 물품이 배정돼서 신청자와 연락해 전달해야 한다 */
public record DeliveryTodo(
		Long itemId,
		String itemName,
		Long reservationId,
		Long applicationId,
		TradeMethod tradeMethod,
		ReservationStatus status,
		LocalDateTime reconfirmationDeadline,
		boolean reconfirmed) {

	/** JPQL 생성자 조회용 — 재확인 여부는 상태로 정한다 */
	public DeliveryTodo(Long itemId, String itemName, Long reservationId, Long applicationId, TradeMethod tradeMethod,
			ReservationStatus status, LocalDateTime reconfirmationDeadline) {
		this(itemId, itemName, reservationId, applicationId, tradeMethod, status, reconfirmationDeadline,
				status == ReservationStatus.RECONFIRMED);
	}
}
