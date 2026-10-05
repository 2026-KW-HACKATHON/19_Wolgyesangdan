package com.Wolgyesangdan.backend.domain.reservation.service;

import com.Wolgyesangdan.backend.domain.reservation.dto.ReservationDetailResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
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

}
