package com.Wolgyesangdan.backend.domain.reservation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReservationDetailResponse;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.exception.ReservationErrorCode;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
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
	void 예약_상세를_조회한다() throws Exception {
		given(reservationService.getReservation(1L, 5L)).willReturn(
				new ReservationDetailResponse(3L, 5L, TradeMethod.CAMPAIGN, LocalDateTime.of(2026, 10, 5, 14, 0),
						ReservationStatus.PICKUP_SCHEDULED, LocalDateTime.of(2026, 10, 4, 23, 59, 59), null,
						new ReservationDetailResponse.Counterpart("월계1동 이웃", ContactType.OPENCHAT, null,
								"https://open.kakao.com/o/xxxxxxx")));

		getReservation("5")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.applicationId").value(5))
				.andExpect(jsonPath("$.tradeMethod").value("CAMPAIGN"))
				.andExpect(jsonPath("$.scheduledAt").value("2026-10-05T14:00:00"))
				.andExpect(jsonPath("$.status").value("PICKUP_SCHEDULED"))
				.andExpect(jsonPath("$.reconfirmationDeadline").value("2026-10-04T23:59:59"))
				.andExpect(jsonPath("$.reconfirmedAt").isEmpty())
				.andExpect(jsonPath("$.counterpart.nickname").value("월계1동 이웃"))
				.andExpect(jsonPath("$.counterpart.contactType").value("OPENCHAT"))
				.andExpect(jsonPath("$.counterpart.phone").isEmpty())
				.andExpect(jsonPath("$.counterpart.openchatLink").value("https://open.kakao.com/o/xxxxxxx"));
	}

	@Test
	void 예약이_없으면_404() throws Exception {
		given(reservationService.getReservation(1L, 5L))
				.willThrow(new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		getReservation("5")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
	}

	@Test
	void 본인과_관련_없는_예약이면_403() throws Exception {
		given(reservationService.getReservation(1L, 5L))
				.willThrow(new BusinessException(ReservationErrorCode.RESERVATION_NOT_PARTICIPANT));

		getReservation("5")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_PARTICIPANT"))
				.andExpect(jsonPath("$.message").value("본인과 관련된 예약이 아닙니다."))
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	@Test
	void applicationId가_숫자가_아니면_400() throws Exception {
		getReservation("abc")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("applicationId"));
	}

	@Test
	void 토큰_없이_조회하면_401() throws Exception {
		mockMvc.perform(get("/applications/5/reservation"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

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

	private ResultActions getReservation(String applicationId) throws Exception {
		return mockMvc.perform(get("/applications/" + applicationId + "/reservation")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L)));
	}

	private ResultActions reconfirm(String reservationId) throws Exception {
		return mockMvc.perform(patch("/reservations/" + reservationId + "/reconfirm")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L)));
	}

}
