package com.Wolgyesangdan.backend.domain.application.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.dto.ApplicationCreateResponse;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.exception.ApplicationErrorCode;
import com.Wolgyesangdan.backend.domain.application.service.ApplicationService;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
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

@WebMvcTest(controllers = ApplicationController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class ApplicationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private ApplicationService applicationService;

	@Test
	void 신청하면_201과_대기_순번을_반환한다() throws Exception {
		given(applicationService.apply(1L, 10L)).willReturn(
				new ApplicationCreateResponse(5L, 10L, ApplicationStatus.WAITING, 2,
						LocalDateTime.of(2026, 9, 26, 15, 30)));

		mockMvc.perform(post("/items/10/applications").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.itemId").value(10))
				.andExpect(jsonPath("$.status").value("WAITING"))
				.andExpect(jsonPath("$.waitlistRank").value(2))
				.andExpect(jsonPath("$.appliedAt").value("2026-09-26T15:30:00"));
	}

	@Test
	void 본인_물품이면_403을_반환한다() throws Exception {
		given(applicationService.apply(1L, 10L))
				.willThrow(new BusinessException(ApplicationErrorCode.APPLICATION_OWN_ITEM));

		mockMvc.perform(post("/items/10/applications").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("APPLICATION_OWN_ITEM"));
	}

	@Test
	void 존재하지_않는_물품이면_404를_반환한다() throws Exception {
		given(applicationService.apply(1L, 10L)).willThrow(new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));

		mockMvc.perform(post("/items/10/applications").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
	}

	@Test
	void 토큰_없이_신청하면_401() throws Exception {
		mockMvc.perform(post("/items/10/applications"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

}
