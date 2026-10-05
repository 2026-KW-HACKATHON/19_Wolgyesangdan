package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;

public record ReconfirmResponse(
		Long id,
		ReservationStatus status,
		LocalDateTime reconfirmedAt) {

	public static ReconfirmResponse from(Reservation reservation) {
		return new ReconfirmResponse(reservation.getId(), reservation.getStatus(), reservation.getReconfirmedAt());
	}

}
