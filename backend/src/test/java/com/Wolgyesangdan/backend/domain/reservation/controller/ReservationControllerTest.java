package com.Wolgyesangdan.backend.domain.reservation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.config.WebConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = ReservationController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class, WebConfig.class})
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservationService reservationService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 수령을_재확인한다() throws Exception {
		given(reservationService.reconfirm(1L, 3L)).willReturn(
				new ReconfirmResponse(3L, ReservationStatus.RECONFIRMED, LocalDateTime.of(2026, 10, 3, 9, 0)));

		reconfirm("3")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.status").value("RECONFIRMED"))
				.andExpect(jsonPath("$.reconfirmedAt").value("2026-10-03T09:00:00"));
	}

	@Test
	void 기한이_지났으면_409() throws Exception {
		given(reservationService.reconfirm(1L, 3L))
				.willThrow(new BusinessException(ReservationErrorCode.RESERVATION_RECONFIRMATION_EXPIRED));

		reconfirm("3")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESERVATION_RECONFIRMATION_EXPIRED"))
				.andExpect(jsonPath("$.message").value("재확인 응답 기한이 지났습니다."))
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	@Test
	void 본인_예약이_아니면_403() throws Exception {
		given(reservationService.reconfirm(1L, 3L))
				.willThrow(new BusinessException(ReservationErrorCode.RESERVATION_NOT_PARTICIPANT));

		reconfirm("3")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_PARTICIPANT"));
	}

	@Test
	void 숫자가_아닌_예약_id는_INVALID_INPUT() throws Exception {
		reconfirm("abc")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("reservationId"));
	}

	@Test
	void 토큰_없이_재확인하면_401() throws Exception {
		mockMvc.perform(patch("/reservations/3/reconfirm"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	private ResultActions reconfirm(String reservationId) throws Exception {
		return mockMvc.perform(patch("/reservations/" + reservationId + "/reconfirm")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L)));
	}

}
