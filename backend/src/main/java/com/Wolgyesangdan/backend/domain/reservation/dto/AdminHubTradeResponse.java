package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;

/**
 * 관리자 거점 거래 목록의 한 줄 — 거점 거래 예약 하나.
 *
 * @param id 예약 id
 * @param atHub 물품이 거점에 들어와 있는지. 입고 여부는 status가 아니라 이 값으로 본다
 *              (신청자가 먼저 수령을 재확인하면 status는 RECONFIRMED가 된다)
 * @param assignedAt 이 신청자에게 배정된 시각 (예약이 만들어진 시각)
 */
public record AdminHubTradeResponse(
		Long id,
		Long itemId,
		String itemName,
		String ownerNickname,
		String applicantNickname,
		ReservationStatus status,
		boolean atHub,
		LocalDateTime hubReceivedAt,
		LocalDateTime reconfirmedAt,
		LocalDateTime reconfirmationDeadline,
		LocalDateTime completedAt,
		LocalDateTime assignedAt) {

	public static AdminHubTradeResponse from(Reservation reservation) {
		Application application = reservation.getApplication();
		Item item = application.getItem();
		return new AdminHubTradeResponse(
				reservation.getId(),
				item.getId(),
				item.getName(),
				item.getOwner().getNickname(),
				application.getApplicant().getNickname(),
				reservation.getStatus(),
				reservation.isAtHub(),
				reservation.getHubReceivedAt(),
				reservation.getReconfirmedAt(),
				reservation.getReconfirmationDeadline(),
				reservation.getCompletedAt(),
				reservation.getCreatedAt());
	}

}
