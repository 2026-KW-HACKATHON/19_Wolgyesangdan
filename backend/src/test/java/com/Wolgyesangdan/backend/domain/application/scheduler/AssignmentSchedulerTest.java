package com.Wolgyesangdan.backend.domain.application.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import com.Wolgyesangdan.backend.domain.application.service.AssignmentService;
import com.Wolgyesangdan.backend.domain.application.service.SuccessionService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AssignmentSchedulerTest {

	private final AssignmentService assignmentService = Mockito.mock(AssignmentService.class);
	private final SuccessionService successionService = Mockito.mock(SuccessionService.class);
	private final AssignmentScheduler scheduler = new AssignmentScheduler(assignmentService, successionService);

	@Test
	void 마감된_물품을_하나씩_배정한다() {
		given(assignmentService.findItemIdsToAssign(any())).willReturn(List.of(1L, 2L));

		scheduler.assignClosedItems();

		then(assignmentService).should().assign(eq(1L), any());
		then(assignmentService).should().assign(eq(2L), any());
	}

	@Test
	void 한_물품이_실패해도_나머지_물품은_처리한다() {
		given(assignmentService.findItemIdsToAssign(any())).willReturn(List.of(1L, 2L));
		given(assignmentService.assign(eq(1L), any())).willThrow(new IllegalStateException("DB 오류"));

		scheduler.assignClosedItems();

		then(assignmentService).should().assign(eq(2L), any());
	}

	@Test
	void 노쇼_승계도_한_건이_실패해도_나머지를_처리한다() {
		given(successionService.findReservationIdsToSucceed(any())).willReturn(List.of(10L, 20L));
		given(successionService.succeed(eq(10L), any())).willThrow(new IllegalStateException("DB 오류"));

		scheduler.succeedNoShows();

		then(successionService).should().succeed(eq(20L), any());
	}

}
