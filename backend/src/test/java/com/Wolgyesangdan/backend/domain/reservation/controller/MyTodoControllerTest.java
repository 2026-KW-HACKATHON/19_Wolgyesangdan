package com.Wolgyesangdan.backend.domain.reservation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.dto.DeliveryTodo;
import com.Wolgyesangdan.backend.domain.reservation.dto.MyTodoResponse;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmTodo;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.reservation.service.ReservationService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.config.WebConfig;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MyTodoController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class, WebConfig.class})
class MyTodoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservationService reservationService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 내가_지금_해야_할_일을_조회한다() throws Exception {
		given(reservationService.getMyTodo(1L)).willReturn(new MyTodoResponse(
				List.of(new ReconfirmTodo(30L, 12L, 2190L, "전자레인지", LocalDateTime.of(2026, 10, 9, 17, 0))),
				List.of(new DeliveryTodo(2188L, "원목 책상", 13L, 31L, TradeMethod.DIRECT,
						ReservationStatus.RECONFIRMED, LocalDateTime.of(2026, 10, 9, 12, 0)))));

		mockMvc.perform(get("/users/me/todo").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reconfirms[0].applicationId").value(30))
				.andExpect(jsonPath("$.reconfirms[0].reservationId").value(12))
				.andExpect(jsonPath("$.reconfirms[0].itemId").value(2190))
				.andExpect(jsonPath("$.reconfirms[0].itemName").value("전자레인지"))
				.andExpect(jsonPath("$.reconfirms[0].reconfirmationDeadline").value("2026-10-09T17:00:00"))
				.andExpect(jsonPath("$.deliveries[0].itemId").value(2188))
				.andExpect(jsonPath("$.deliveries[0].tradeMethod").value("DIRECT"))
				.andExpect(jsonPath("$.deliveries[0].status").value("RECONFIRMED"))
				.andExpect(jsonPath("$.deliveries[0].reconfirmed").value(true));
	}

	@Test
	void 비로그인이면_401() throws Exception {
		mockMvc.perform(get("/users/me/todo"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

}
