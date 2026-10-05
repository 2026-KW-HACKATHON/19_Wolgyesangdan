package com.Wolgyesangdan.backend.domain.application.service;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.repository.ApplicationRepository;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재확인 기한 초과·노쇼 → 다음 대기자 승계 (요구사항 ASGN-04·05). AssignmentScheduler가 1분마다 호출한다.
 */
@Service
@RequiredArgsConstructor
public class SuccessionService {

	private final ItemRepository itemRepository;
	private final ApplicationRepository applicationRepository;
	private final ReservationRepository reservationRepository;

	public enum Result {
		/** 다음 대기자에게 배정을 넘겼다 */
		SUCCEEDED,
		/** 넘길 대기자가 없어 물품을 종료했다 */
		CANCELED,
		/** 이미 처리됐거나 승계 대상이 아니라 건너뛰었다 */
		SKIPPED
	}

	@Transactional(readOnly = true)
	public List<Long> findReservationIdsToSucceed(LocalDateTime now) {
		return reservationRepository.findIdsToSucceed(ReservationService.RECONFIRMABLE_STATUSES, now);
	}

	/**
	 * 예약 하나를 승계 처리한다. 물품 행을 먼저 잠근 뒤 예약·신청을 읽어서 상태를 다시 확인하므로,
	 * 스케줄러가 겹쳐 돌거나 재확인·신청 취소와 동시에 일어나도 한 번만 승계된다.
	 * - 재확인 기한이 지난 미확인 예약은 NO_SHOW로 표시한다 (운영진이 이미 NO_SHOW로 바꾼 예약은 그대로)
	 * - 노쇼 난 신청은 CANCELED, 다음 대기자(우선배정 점수 → 신청 시각 → id)를 SELECTED로 올리고 새 예약을 만든다
	 * - 넘길 대기자가 없으면 물품을 종료한다
	 */
	@Transactional
	public Result succeed(Long reservationId, LocalDateTime now) {
		Long itemId = reservationRepository.findItemIdById(reservationId).orElse(null);
		if (itemId == null) {
			return Result.SKIPPED;
		}
		Item item = itemRepository.findByIdForUpdate(itemId).orElseThrow();
		Reservation reservation = reservationRepository.findWithApplicationById(reservationId).orElseThrow();
		Application dropped = reservation.getApplication();
		if (!needsSuccession(reservation, now)) {
			return Result.SKIPPED;
		}

		ReservationStatus nextStatus = successorStatus(reservation);
		reservation.markNoShow();
		dropped.dropForNoShow();

		List<Application> waiting = applicationRepository
				.findByItemAndStatusOrderByPriorityScoreDescCreatedAtAscIdAsc(item, ApplicationStatus.WAITING);
		if (waiting.isEmpty()) {
			item.cancel();
			return Result.CANCELED;
		}
		Application next = waiting.get(0);
		next.select(now);
		reservationRepository.save(Reservation.builder()
				.application(next)
				.tradeMethod(reservation.getTradeMethod())
				.status(nextStatus)
				.reconfirmationDeadline(now.plus(AssignmentService.RECONFIRMATION_PERIOD))
				.build());
		return Result.SUCCEEDED;
	}

	private static boolean needsSuccession(Reservation reservation, LocalDateTime now) {
		if (reservation.getApplication().getStatus() != ApplicationStatus.SELECTED) {
			return false; // 이미 승계했거나 거래가 끝난 예약
		}
		if (reservation.getStatus() == ReservationStatus.NO_SHOW) {
			return true; // 운영진이 노쇼로 표시
		}
		LocalDateTime deadline = reservation.getReconfirmationDeadline();
		return ReservationService.RECONFIRMABLE_STATUSES.contains(reservation.getStatus())
				&& deadline != null && deadline.isBefore(now);
	}

	/**
	 * 다음 배정자의 예약 첫 상태. 직거래는 SCHEDULED. 거점 거래는 등록자가 아직 거점에 맡기기 전(HUB_DROP_SCHEDULED)이면
	 * 그대로 맡기기 예정, 이미 거점에 들어온 뒤(입고·수령 예정·운영진 노쇼 표시)면 바로 수령 예정(PICKUP_SCHEDULED)이다.
	 */
	private static ReservationStatus successorStatus(Reservation reservation) {
		if (reservation.getTradeMethod() == TradeMethod.DIRECT) {
			return ReservationStatus.SCHEDULED;
		}
		return reservation.getStatus() == ReservationStatus.HUB_DROP_SCHEDULED
				? ReservationStatus.HUB_DROP_SCHEDULED
				: ReservationStatus.PICKUP_SCHEDULED;
	}

}
