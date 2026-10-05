package com.Wolgyesangdan.backend.domain.application.scheduler;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.service.AssignmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 신청 마감이 지난 물품을 1분마다 찾아 배정한다. 물품마다 트랜잭션을 따로 써서,
 * 한 물품 처리가 실패해도 나머지 물품은 계속 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentScheduler {

	private final AssignmentService assignmentService;

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

}
