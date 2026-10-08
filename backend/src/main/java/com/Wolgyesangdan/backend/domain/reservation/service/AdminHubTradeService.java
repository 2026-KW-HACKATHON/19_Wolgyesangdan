package com.Wolgyesangdan.backend.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import com.Wolgyesangdan.backend.domain.application.service.SuccessionService;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.reservation.dto.AdminHubTradeResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 거점 거래 처리 (#252). 거점 거래는 등록자가 거점에 맡기고 신청자가 거점에서 받아 가므로,
 * 운영진이 입고 → 수령 완료(또는 미수령)를 기록한다. 직거래는 대상이 아니다 (등록자가 앱에서 전달 완료).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHubTradeService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
	private static final Set<ReservationStatus> CLOSED_STATUSES =
			EnumSet.of(ReservationStatus.COMPLETED, ReservationStatus.NO_SHOW, ReservationStatus.CANCELED);

	private final ReservationRepository reservationRepository;
	private final ItemRepository itemRepository;
	private final SuccessionService successionService;

	/**
	 * 거점 거래 목록 — 최근 배정순 (요청의 sort는 쓰지 않는다). 한 페이지는 최대 100개.
	 *
	 * @param done true면 끝난 거래(완료·미수령·취소)만, false면 진행 중인 거래만, null이면 전부
	 */
	public Page<AdminHubTradeResponse> getHubTrades(Boolean done, Pageable pageable) {
		return reservationRepository.findHubTrades(done, CLOSED_STATUSES,
						PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), LATEST))
				.map(AdminHubTradeResponse::from);
	}

	/** 거점 입고 — 등록자가 맡긴 물품을 거점에서 받았다 */
	@Transactional
	public AdminHubTradeResponse receive(Long reservationId) {
		return receive(reservationId, LocalDateTime.now());
	}

	AdminHubTradeResponse receive(Long reservationId, LocalDateTime now) {
		lockItem(reservationId);
		Reservation reservation = findOpenHubTrade(reservationId);
		if (reservation.isAtHub()) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_AT_HUB);
		}
		reservation.receiveAtHub(now);
		return AdminHubTradeResponse.from(reservation);
	}

	/**
	 * 수령 완료 — 신청자가 거점에서 물품을 받아 갔다. 예약·신청·물품을 거래 완료로 바꾼다 (직거래의 "전달 완료"와 같은 결과).
	 * 거점에 받으러 온 것 자체가 수령 의사라서, 신청자가 앱에서 재확인하지 않았어도 완료할 수 있다.
	 */
	@Transactional
	public AdminHubTradeResponse complete(Long reservationId) {
		return complete(reservationId, LocalDateTime.now());
	}

	AdminHubTradeResponse complete(Long reservationId, LocalDateTime now) {
		Item item = lockItem(reservationId);
		Reservation reservation = findOpenHubTrade(reservationId);
		if (!reservation.isAtHub()) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_AT_HUB);
		}
		reservation.complete(now);
		reservation.getApplication().complete();
		item.complete();
		return AdminHubTradeResponse.from(reservation);
	}

	/**
	 * 미수령 — 신청자가 거점에 받으러 오지 않았다. 노쇼로 표시하고 바로 다음 대기자에게 넘긴다
	 * (대기자가 없으면 물품을 종료한다). 물품은 거점에 그대로 있으므로 다음 배정자의 예약은 "수령 예정"으로 시작한다.
	 */
	@Transactional
	public AdminHubTradeResponse markNoShow(Long reservationId) {
		return markNoShow(reservationId, LocalDateTime.now());
	}

	AdminHubTradeResponse markNoShow(Long reservationId, LocalDateTime now) {
		lockItem(reservationId);
		Reservation reservation = findOpenHubTrade(reservationId);
		if (!reservation.isAtHub()) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_AT_HUB);
		}
		reservation.markNoShow();
		// 승계 스케줄러를 기다리지 않고 바로 넘긴다 — 운영진이 표시한 노쇼는 SuccessionService가 승계 대상으로 본다
		successionService.succeed(reservationId, now);
		return AdminHubTradeResponse.from(reservation);
	}

	// 회원의 재확인·승계 스케줄러와 겹치지 않도록 물품부터 잠근다 (ReservationService·SuccessionService와 같은 순서)
	private Item lockItem(Long reservationId) {
		Long itemId = reservationRepository.findItemIdById(reservationId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		return itemRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
	}

	// 처리할 수 있는 예약 — 거점 거래이고 아직 끝나지 않은 것
	private Reservation findOpenHubTrade(Long reservationId) {
		Reservation reservation = reservationRepository.findWithParticipantsById(reservationId)
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
		if (reservation.getTradeMethod() != TradeMethod.CAMPAIGN) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_HUB_TRADE);
		}
		if (reservation.isClosed()) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_CLOSED);
		}
		return reservation;
	}

}
