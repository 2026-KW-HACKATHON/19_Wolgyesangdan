package com.Wolgyesangdan.backend.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.EnumSet;

import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReservationDetailResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

	/** 재확인할 수 있는 상태 — 아직 전달·수령 전인 예약 (2026-10-05 결정, #65) */
	static final EnumSet<ReservationStatus> RECONFIRMABLE_STATUSES = EnumSet.of(
			ReservationStatus.SCHEDULED,
			ReservationStatus.HUB_DROP_SCHEDULED,
			ReservationStatus.HUB_RECEIVED,
			ReservationStatus.PICKUP_SCHEDULED);

	private final ReservationRepository reservationRepository;

	/**
	 * 신청에 딸린 예약 상세. 그 신청의 신청자와 물품 등록자만 볼 수 있고, 서로 상대방의 닉네임·연락 수단을 받는다.
	 * 연락 수단이 공개되는 유일한 API다 (물품 목록·상세에는 내려주지 않는다).
	 * 신청이 없거나 아직 배정 전이라 예약이 없으면 둘 다 RESERVATION_NOT_FOUND.
	 */
	public ReservationDetailResponse getReservation(Long userId, Long applicationId) {
		Reservation reservation = reservationRepository.findWithParticipantsByApplicationId(applicationId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		User applicant = reservation.getApplication().getApplicant();
		User owner = reservation.getApplication().getItem().getOwner();
		if (applicant.getId().equals(userId)) {
			return ReservationDetailResponse.of(reservation, owner);
		}
		if (owner.getId().equals(userId)) {
			return ReservationDetailResponse.of(reservation, applicant);
		}
		throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_PARTICIPANT);
	}

	/**
	 * 수령 재확인. 배정된 신청자 본인만, 진행 중인 예약에 대해 기한 안에 할 수 있다.
	 * 재확인 기한이 비어 있으면 기한 없음으로 보고 허용한다 (기한 계산은 스케줄러에서 정할 예정).
	 */
	@Transactional
	public ReconfirmResponse reconfirm(Long userId, Long reservationId) {
		return reconfirm(userId, reservationId, LocalDateTime.now());
	}

	ReconfirmResponse reconfirm(Long userId, Long reservationId, LocalDateTime now) {
		Reservation reservation = reservationRepository.findWithApplicationById(reservationId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		if (!reservation.getApplication().getApplicant().getId().equals(userId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_PARTICIPANT);
		}
		if (reservation.getStatus() == ReservationStatus.RECONFIRMED) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_RECONFIRMED);
		}
		if (!RECONFIRMABLE_STATUSES.contains(reservation.getStatus())) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_RECONFIRMABLE);
		}
		LocalDateTime deadline = reservation.getReconfirmationDeadline();
		if (deadline != null && now.isAfter(deadline)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_RECONFIRMATION_EXPIRED);
		}

		reservation.reconfirm(now);
		return ReconfirmResponse.from(reservation);
	}

}
