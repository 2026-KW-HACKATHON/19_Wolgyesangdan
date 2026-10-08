package com.Wolgyesangdan.backend.domain.dashboard.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.dashboard.dto.AdminSummaryResponse;
import com.Wolgyesangdan.backend.domain.dashboard.service.AdminSummaryService;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
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

@WebMvcTest(controllers = AdminSummaryController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class AdminSummaryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminSummaryService adminSummaryService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 관리자가_요약을_조회한다() throws Exception {
		given(adminSummaryService.getSummary()).willReturn(new AdminSummaryResponse(3, 5, 2,
				new AdminSummaryResponse.CurrentCampaign(1L, "2026 가을 캠페인", CampaignStatus.ACTIVE,
						LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 6), 128)));

		mockMvc.perform(get("/admin/summary").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pendingVerifications").value(3))
				.andExpect(jsonPath("$.openInquiries").value(5))
				.andExpect(jsonPath("$.hiddenItems").value(2))
				.andExpect(jsonPath("$.currentCampaign.id").value(1))
				.andExpect(jsonPath("$.currentCampaign.name").value("2026 가을 캠페인"))
				.andExpect(jsonPath("$.currentCampaign.status").value("ACTIVE"))
				.andExpect(jsonPath("$.currentCampaign.startDate").value("2026-09-28"))
				.andExpect(jsonPath("$.currentCampaign.endDate").value("2026-11-06"))
				.andExpect(jsonPath("$.currentCampaign.reusedCount").value(128));
	}

	@Test
	void 캠페인이_없으면_currentCampaign은_null() throws Exception {
		given(adminSummaryService.getSummary()).willReturn(new AdminSummaryResponse(0, 0, 0, null));

		mockMvc.perform(get("/admin/summary").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pendingVerifications").value(0))
				.andExpect(jsonPath("$.currentCampaign").isEmpty());
	}

	@Test
	void 비로그인이면_401() throws Exception {
		mockMvc.perform(get("/admin/summary"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 일반_회원이면_403_AUTH_FORBIDDEN() throws Exception {
		mockMvc.perform(get("/admin/summary")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(7L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
	}

	private String adminToken() {
		return "Bearer " + jwtProvider.createAccessToken(1L, Role.ADMIN);
	}

}
