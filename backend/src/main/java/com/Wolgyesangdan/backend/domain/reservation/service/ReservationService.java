package com.Wolgyesangdan.backend.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.EnumSet;

import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.dto.CompleteResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.MyTodoResponse;
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
	public static final EnumSet<ReservationStatus> RECONFIRMABLE_STATUSES = EnumSet.of(
			ReservationStatus.SCHEDULED,
			ReservationStatus.HUB_DROP_SCHEDULED,
			ReservationStatus.HUB_RECEIVED,
			ReservationStatus.PICKUP_SCHEDULED);

	/** 등록자가 전달을 마쳐야 하는 상태 — 끝나지 않은(완료·노쇼·취소 아님) 예약 */
	private static final EnumSet<ReservationStatus> DELIVERY_PENDING_STATUSES = EnumSet.of(
			ReservationStatus.SCHEDULED,
			ReservationStatus.HUB_DROP_SCHEDULED,
			ReservationStatus.HUB_RECEIVED,
			ReservationStatus.PICKUP_SCHEDULED,
			ReservationStatus.RECONFIRMED);

	private final ReservationRepository reservationRepository;
	private final ItemRepository itemRepository;

	/** 내가 지금 해야 할 일 — 신청자로서 수령 재확인, 등록자로서 전달 (#187). 쿼리 2번 */
	public MyTodoResponse getMyTodo(Long userId) {
		return getMyTodo(userId, LocalDateTime.now());
	}

	MyTodoResponse getMyTodo(Long userId, LocalDateTime now) {
		return new MyTodoResponse(
				reservationRepository.findReconfirmTodos(userId, RECONFIRMABLE_STATUSES, now),
				reservationRepository.findDeliveryTodos(userId, DELIVERY_PENDING_STATUSES));
	}

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
		// 노쇼 승계 스케줄러와 겹치지 않도록 물품부터 잠근다 (SuccessionService와 같은 순서) —
		// 기한 직전 재확인과 기한 직후 승계가 동시에 일어나도 한쪽만 반영된다
		Long itemId = reservationRepository.findItemIdById(reservationId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		itemRepository.findByIdForUpdate(itemId);
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

	/**
	 * 직거래 전달 완료 (2026-10-05 결정, #100). 물품 등록자만, 신청자가 수령을 재확인한 직거래 예약만 완료할 수 있다.
	 * 예약·신청·물품을 모두 COMPLETED로 바꿔서 탄소 리포트·자원순환 기록·전달 완료 횟수에 집계되게 한다.
	 * 노쇼 승계 스케줄러와 같은 순서로 물품부터 잠근다.
	 */
	@Transactional
	public CompleteResponse complete(Long userId, Long reservationId) {
		return complete(userId, reservationId, LocalDateTime.now());
	}

	CompleteResponse complete(Long userId, Long reservationId, LocalDateTime now) {
		Long itemId = reservationRepository.findItemIdById(reservationId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		Item item = itemRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		Reservation reservation = reservationRepository.findWithApplicationById(reservationId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		if (!item.getOwner().getId().equals(userId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_ITEM_OWNER);
		}
		if (reservation.getStatus() == ReservationStatus.COMPLETED) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_COMPLETED);
		}
		if (reservation.getTradeMethod() != TradeMethod.DIRECT) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_DIRECT);
		}
		if (reservation.getStatus() == ReservationStatus.SCHEDULED) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_RECONFIRMED);
		}
		if (reservation.getStatus() != ReservationStatus.RECONFIRMED) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_COMPLETABLE); // 노쇼·취소된 예약
		}

		reservation.complete(now);
		reservation.getApplication().complete();
		item.complete();
		return CompleteResponse.from(reservation);
	}

}
