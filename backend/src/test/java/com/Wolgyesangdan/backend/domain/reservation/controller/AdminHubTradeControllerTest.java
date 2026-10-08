package com.Wolgyesangdan.backend.domain.reservation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.reservation.dto.AdminHubTradeResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.service.AdminHubTradeService;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminHubTradeController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class AdminHubTradeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminHubTradeService adminHubTradeService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 관리자가_거점_거래_목록을_페이지_형식으로_조회한다() throws Exception {
		given(adminHubTradeService.getHubTrades(null, PageRequest.of(0, 20))).willReturn(new PageImpl<>(
				List.of(trade(ReservationStatus.HUB_RECEIVED, true)), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/admin/hub-trades").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(7))
				.andExpect(jsonPath("$.content[0].itemId").value(3))
				.andExpect(jsonPath("$.content[0].itemName").value("1인용 책상"))
				.andExpect(jsonPath("$.content[0].ownerNickname").value("등록자"))
				.andExpect(jsonPath("$.content[0].applicantNickname").value("신청자"))
				.andExpect(jsonPath("$.content[0].status").value("HUB_RECEIVED"))
				.andExpect(jsonPath("$.content[0].atHub").value(true))
				.andExpect(jsonPath("$.content[0].hubReceivedAt").value("2026-10-08T15:00:00"))
				.andExpect(jsonPath("$.content[0].reconfirmedAt").isEmpty())
				.andExpect(jsonPath("$.content[0].reconfirmationDeadline").value("2026-10-09T14:00:00"))
				.andExpect(jsonPath("$.content[0].assignedAt").value("2026-10-08T14:00:00"))
				// 연락처는 내려주지 않는다
				.andExpect(jsonPath("$.content[0].phone").doesNotExist())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void done_필터와_page_size를_넘긴다() throws Exception {
		given(adminHubTradeService.getHubTrades(false, PageRequest.of(1, 5)))
				.willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

		mockMvc.perform(get("/admin/hub-trades").header(HttpHeaders.AUTHORIZATION, adminToken())
						.param("done", "false").param("page", "1").param("size", "5"))
				.andExpect(status().isOk());

		verify(adminHubTradeService).getHubTrades(false, PageRequest.of(1, 5));
	}

	@Test
	void 입고_수령_완료_미수령을_처리한다() throws Exception {
		given(adminHubTradeService.receive(7L)).willReturn(trade(ReservationStatus.HUB_RECEIVED, true));
		given(adminHubTradeService.complete(7L)).willReturn(trade(ReservationStatus.COMPLETED, true));
		given(adminHubTradeService.markNoShow(7L)).willReturn(trade(ReservationStatus.NO_SHOW, true));

		mockMvc.perform(post("/admin/hub-trades/7/receive").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("HUB_RECEIVED"))
				.andExpect(jsonPath("$.atHub").value(true));
		mockMvc.perform(post("/admin/hub-trades/7/complete").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
		mockMvc.perform(post("/admin/hub-trades/7/no-show").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NO_SHOW"));
	}

	@Test
	void 입고_전에_수령_완료하면_409_RESERVATION_NOT_AT_HUB() throws Exception {
		given(adminHubTradeService.complete(7L)).willThrow(new BusinessException(ReservationErrorCode.RESERVATION_NOT_AT_HUB));

		mockMvc.perform(post("/admin/hub-trades/7/complete").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_AT_HUB"))
				.andExpect(jsonPath("$.message").value("거점에 입고된 뒤에 처리할 수 있습니다."));
	}

	@Test
	void 없는_예약이면_404_RESERVATION_NOT_FOUND() throws Exception {
		given(adminHubTradeService.receive(999L)).willThrow(new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		mockMvc.perform(post("/admin/hub-trades/999/receive").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
	}

	@Test
	void 비로그인이면_401() throws Exception {
		mockMvc.perform(get("/admin/hub-trades"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 일반_회원이면_403_AUTH_FORBIDDEN() throws Exception {
		String userToken = "Bearer " + jwtProvider.createAccessToken(7L);

		mockMvc.perform(get("/admin/hub-trades").header(HttpHeaders.AUTHORIZATION, userToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
		for (String action : List.of("receive", "complete", "no-show")) {
			mockMvc.perform(post("/admin/hub-trades/7/" + action).header(HttpHeaders.AUTHORIZATION, userToken))
					.andExpect(status().isForbidden());
		}
	}

	private String adminToken() {
		return "Bearer " + jwtProvider.createAccessToken(1L, Role.ADMIN);
	}

	private static AdminHubTradeResponse trade(ReservationStatus status, boolean atHub) {
		return new AdminHubTradeResponse(7L, 3L, "1인용 책상", "등록자", "신청자", status, atHub,
				atHub ? LocalDateTime.of(2026, 10, 8, 15, 0, 0) : null, null,
				LocalDateTime.of(2026, 10, 9, 14, 0, 0), null, LocalDateTime.of(2026, 10, 8, 14, 0, 0));
	}

}
