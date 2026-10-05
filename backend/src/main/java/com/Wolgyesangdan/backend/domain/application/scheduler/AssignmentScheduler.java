package com.Wolgyesangdan.backend.domain.application.scheduler;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.service.AssignmentService;
import com.Wolgyesangdan.backend.domain.application.service.SuccessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1분마다 (1) 신청 마감이 지난 물품을 배정하고 (2) 재확인 기한 초과·노쇼 예약을 다음 대기자에게 넘긴다.
 * 건마다 트랜잭션을 따로 써서, 한 건 처리가 실패해도 나머지는 계속 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentScheduler {

	private final AssignmentService assignmentService;
	private final SuccessionService successionService;

	@Scheduled(fixedDelayString = "${scheduler.assignment.fixed-delay:PT1M}",
			initialDelayString = "${scheduler.assignment.initial-delay:PT30S}")
	public void assignClosedItems() {
		LocalDateTime now = LocalDateTime.now();
		for (Long itemId : assignmentService.findItemIdsToAssign(now)) {
			try {
				AssignmentService.Result result = assignmentService.assign(itemId, now);
				if (result != AssignmentService.Result.SKIPPED) {
					log.info("물품 {} 마감 처리: {}", itemId, result);
				}
			} catch (RuntimeException e) {
				log.error("물품 {} 배정 실패 — 다음 실행에서 다시 시도", itemId, e);
			}
		}
	}

	@Scheduled(fixedDelayString = "${scheduler.succession.fixed-delay:PT1M}",
			initialDelayString = "${scheduler.succession.initial-delay:PT40S}")
	public void succeedNoShows() {
		LocalDateTime now = LocalDateTime.now();
		for (Long reservationId : successionService.findReservationIdsToSucceed(now)) {
			try {
				SuccessionService.Result result = successionService.succeed(reservationId, now);
				if (result != SuccessionService.Result.SKIPPED) {
					log.info("예약 {} 노쇼 승계: {}", reservationId, result);
				}
			} catch (RuntimeException e) {
				log.error("예약 {} 승계 실패 — 다음 실행에서 다시 시도", reservationId, e);
			}
		}
	}

}
