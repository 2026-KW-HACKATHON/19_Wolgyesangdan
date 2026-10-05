package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.entity.User;

/**
 * 예약 상세. counterpart는 조회자의 거래 상대 — 신청자가 보면 등록자, 등록자가 보면 신청자.
 */
public record ReservationDetailResponse(
		Long id,
		Long applicationId,
		TradeMethod tradeMethod,
		LocalDateTime scheduledAt,
		ReservationStatus status,
		LocalDateTime reconfirmationDeadline,
		LocalDateTime reconfirmedAt,
		Counterpart counterpart) {

	public static ReservationDetailResponse of(Reservation reservation, User counterpart) {
		return new ReservationDetailResponse(
				reservation.getId(),
				reservation.getApplication().getId(),
				reservation.getTradeMethod(),
				reservation.getScheduledAt(),
				reservation.getStatus(),
				reservation.getReconfirmationDeadline(),
				reservation.getReconfirmedAt(),
				Counterpart.from(counterpart));
	}

	public record Counterpart(String nickname, ContactType contactType, String phone, String openchatLink) {

		// users에는 두 연락 수단이 다 남아 있을 수 있다 — 상대에게는 공개하기로 고른 쪽만 내려준다
		static Counterpart from(User user) {
			ContactType contactType = user.getContactType();
			return new Counterpart(
					user.getNickname(),
					contactType,
					contactType == ContactType.PHONE ? user.getPhone() : null,
					contactType == ContactType.OPENCHAT ? user.getOpenchatLink() : null);
		}
	}

}
