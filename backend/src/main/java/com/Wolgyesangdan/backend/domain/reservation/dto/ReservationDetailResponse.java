package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

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

	/** 거래가 깨진 예약 — 상태는 보여주되 상대 연락처는 더 이상 내려주지 않는다 (2026-10-05 결정, #63) */
	private static final Set<ReservationStatus> CONTACT_HIDDEN_STATUSES =
			EnumSet.of(ReservationStatus.NO_SHOW, ReservationStatus.CANCELED);

	public static ReservationDetailResponse of(Reservation reservation, User counterpart) {
		boolean showContact = !CONTACT_HIDDEN_STATUSES.contains(reservation.getStatus());
		return new ReservationDetailResponse(
				reservation.getId(),
				reservation.getApplication().getId(),
				reservation.getTradeMethod(),
				reservation.getScheduledAt(),
				reservation.getStatus(),
				reservation.getReconfirmationDeadline(),
				reservation.getReconfirmedAt(),
				Counterpart.from(counterpart, showContact));
	}

	public record Counterpart(String nickname, ContactType contactType, String phone, String openchatLink) {

		// users에는 두 연락 수단이 다 남아 있을 수 있다 — 상대에게는 공개하기로 고른 쪽만 내려준다
		static Counterpart from(User user, boolean showContact) {
			ContactType contactType = user.getContactType();
			return new Counterpart(
					user.getNickname(),
					contactType,
					showContact && contactType == ContactType.PHONE ? user.getPhone() : null,
					showContact && contactType == ContactType.OPENCHAT ? user.getOpenchatLink() : null);
		}
	}

}
