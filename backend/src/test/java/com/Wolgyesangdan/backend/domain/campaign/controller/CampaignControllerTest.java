package com.Wolgyesangdan.backend.domain.campaign.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.dto.ActiveCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CampaignController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class CampaignControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CampaignService campaignService;

	@Test
	void 비로그인으로_진행_중인_캠페인을_조회한다() throws Exception {
		given(campaignService.getActiveCampaign()).willReturn(new ActiveCampaignResponse(1L, "2026 자원순환 캠페인",
				"소개", CampaignStatus.ACTIVE,
				LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 4),
				LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 4),
				LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 6),
				"광운대 비마관 1층 거점", "서울 노원구 광운로 20", "평일 10:00 ~ 18:00", 128, 3420));

		mockMvc.perform(get("/campaigns/active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.registrationStartDate").value("2026-09-15"))
				.andExpect(jsonPath("$.pickupEndDate").value("2026-10-06"))
				.andExpect(jsonPath("$.reusedCount").value(128))
				.andExpect(jsonPath("$.carbonReductionKg").value(3420));
	}

	@Test
	void 캠페인이_없으면_200과_JSON_null() throws Exception {
		given(campaignService.getActiveCampaign()).willReturn(null);

		mockMvc.perform(get("/campaigns/active"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().string("null"));
	}

}
