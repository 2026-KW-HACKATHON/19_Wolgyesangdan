package com.Wolgyesangdan.backend.domain.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.repository.ApplicationRepository;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemTradeMethodRepository;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청 마감 → 배정 (요구사항 ASGN-01·02·05). AssignmentScheduler가 1분마다 호출한다.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

	/** 배정 시각부터 수령 재확인까지 주는 시간 (2026-10-05 결정, #91) */
	static final Duration RECONFIRMATION_PERIOD = Duration.ofHours(24);

	/** 아직 배정하지 않은 상태 — 신청 받는 중이거나 정원이 차서 마감된 물품 */
	private static final EnumSet<ItemStatus> ASSIGNABLE_STATUSES = EnumSet.of(ItemStatus.OPEN, ItemStatus.CLOSED);

	private final ItemRepository itemRepository;
	private final ItemTradeMethodRepository itemTradeMethodRepository;
	private final ApplicationRepository applicationRepository;
	private final ReservationRepository reservationRepository;

	public enum Result {
		/** 1순위를 배정하고 예약을 만들었다 */
		ASSIGNED,
		/** 신청자가 없어 물품을 종료했다 */
		CANCELED,
		/** 이미 처리됐거나 아직 마감 전이라 건너뛰었다 */
		SKIPPED
	}

	@Transactional(readOnly = true)
	public List<Long> findItemIdsToAssign(LocalDateTime now) {
		return itemRepository.findIdsToAssign(ASSIGNABLE_STATUSES, now);
	}

	/**
	 * 물품 하나를 배정한다. 물품 행을 잠근 뒤 상태·마감을 다시 확인해서, 스케줄러가 겹쳐 돌거나
	 * 같은 물품을 두 번 넘겨받아도 한 번만 배정된다 (ASGN-05).
	 * - 1순위(우선배정 점수 → 신청 시각 → id) 신청을 SELECTED로, 나머지는 대기자로 그대로 둔다
	 * - 예약 거래 방식은 물품이 거점 거래를 포함하면 CAMPAIGN, 아니면 DIRECT
	 * - 재확인 기한은 배정 시각 + 24시간, 전달 예정 일시는 비워둔다 (두 사람이 연락처로 직접 정함)
	 * - 신청자가 없으면 물품을 종료한다
	 */
	@Transactional
	public Result assign(Long itemId, LocalDateTime now) {
		Item item = itemRepository.findByIdForUpdate(itemId).orElse(null);
		if (item == null || !ASSIGNABLE_STATUSES.contains(item.getStatus())
				|| !item.getApplicationDeadline().isBefore(now)) {
			return Result.SKIPPED;
		}

		List<Application> waiting = applicationRepository
				.findByItemAndStatusOrderByPriorityScoreDescCreatedAtAscIdAsc(item, ApplicationStatus.WAITING);
		if (waiting.isEmpty()) {
			item.cancel();
			return Result.CANCELED;
		}

		Application selected = waiting.get(0);
		selected.select(now);
		TradeMethod tradeMethod = tradeMethodOf(itemId);
		reservationRepository.save(Reservation.builder()
				.application(selected)
				.tradeMethod(tradeMethod)
				.status(tradeMethod == TradeMethod.CAMPAIGN
						? ReservationStatus.HUB_DROP_SCHEDULED
						: ReservationStatus.SCHEDULED)
				.reconfirmationDeadline(now.plus(RECONFIRMATION_PERIOD))
				.build());
		item.assign();
		return Result.ASSIGNED;
	}

	// 거점 거래로 등록한 물품은 신청 마감도 캠페인 일정에 맞춰져 있어서 거점으로 진행한다
	private TradeMethod tradeMethodOf(Long itemId) {
		boolean campaign = itemTradeMethodRepository.findByItemId(itemId).stream()
				.anyMatch(method -> method.getTradeMethod() == TradeMethod.CAMPAIGN);
		return campaign ? TradeMethod.CAMPAIGN : TradeMethod.DIRECT;
	}

}
