package com.Wolgyesangdan.backend.domain.carbonreport.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Wolgyesangdan.backend.domain.carbonreport.dto.MyImpactResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.service.CarbonReportService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MyImpactController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class MyImpactControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private CarbonReportService carbonReportService;

	@Test
	void 로그인한_사용자의_자원순환_기록을_조회한다() throws Exception {
		given(carbonReportService.getMyImpact(1L)).willReturn(new MyImpactResponse(3, 1, 58));

		mockMvc.perform(get("/users/me/impact")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.givenCount").value(3))
				.andExpect(jsonPath("$.receivedCount").value(1))
				.andExpect(jsonPath("$.carbonReductionKg").value(58));
	}

	@Test
	void 토큰_없이_조회하면_401() throws Exception {
		mockMvc.perform(get("/users/me/impact"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

}
