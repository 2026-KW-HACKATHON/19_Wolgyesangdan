package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;

public record CompleteResponse(
		Long id,
		ReservationStatus status,
		LocalDateTime completedAt) {

	public static CompleteResponse from(Reservation reservation) {
		return new CompleteResponse(reservation.getId(), reservation.getStatus(), reservation.getCompletedAt());
	}

}
