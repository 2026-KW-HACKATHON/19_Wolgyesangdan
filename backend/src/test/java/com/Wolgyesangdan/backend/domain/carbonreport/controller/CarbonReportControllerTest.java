package com.Wolgyesangdan.backend.domain.carbonreport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.CategoryCarbon;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.DailyTrade;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.MonthlyCarbon;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.MyContribution;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.CarbonReportResponse.ReportCampaign;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.ReportScope;
import com.Wolgyesangdan.backend.domain.carbonreport.service.CarbonReportService;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
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

@WebMvcTest(controllers = CarbonReportController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class CarbonReportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private CarbonReportService carbonReportService;

	@Test
	void 비로그인으로_scope_없이_조회하면_전체_범위로_집계한다() throws Exception {
		given(carbonReportService.getReport(ReportScope.ALL, null)).willReturn(new CarbonReportResponse(
				ReportScope.ALL, 612, 15840, "2025-03", null,
				List.of(new MonthlyCarbon("2026-10", 980)), List.of(),
				List.of(new CategoryCarbon(CategoryGroup.FURNITURE, 6120, 0.39)), null));

		mockMvc.perform(get("/carbon-report"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope").value("ALL"))
				.andExpect(jsonPath("$.reusedCount").value(612))
				.andExpect(jsonPath("$.carbonReductionKg").value(15840))
				.andExpect(jsonPath("$.since").value("2025-03"))
				.andExpect(jsonPath("$.campaign").isEmpty())
				.andExpect(jsonPath("$.monthlyTrend[0].month").value("2026-10"))
				.andExpect(jsonPath("$.monthlyTrend[0].carbonReductionKg").value(980))
				.andExpect(jsonPath("$.dailyTrades").isEmpty())
				.andExpect(jsonPath("$.categoryBreakdown[0].categoryGroup").value("가구"))
				.andExpect(jsonPath("$.categoryBreakdown[0].ratio").value(0.39))
				.andExpect(jsonPath("$.me").isEmpty());
	}

	@Test
	void 로그인하면_캠페인_범위와_내_기여분을_함께_내려준다() throws Exception {
		given(carbonReportService.getReport(ReportScope.CAMPAIGN, 7L)).willReturn(new CarbonReportResponse(
				ReportScope.CAMPAIGN, 128, 3420, "2026-09",
				new ReportCampaign(1L, "2026 자원순환 캠페인", CampaignStatus.ACTIVE,
						LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 4)),
				List.of(), List.of(new DailyTrade(LocalDate.of(2026, 9, 20), 6)), List.of(), new MyContribution(41)));

		mockMvc.perform(get("/carbon-report").param("scope", "CAMPAIGN")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(7L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope").value("CAMPAIGN"))
				.andExpect(jsonPath("$.campaign.name").value("2026 자원순환 캠페인"))
				.andExpect(jsonPath("$.campaign.status").value("ACTIVE"))
				.andExpect(jsonPath("$.campaign.startDate").value("2026-09-20"))
				.andExpect(jsonPath("$.dailyTrades[0].date").value("2026-09-20"))
				.andExpect(jsonPath("$.dailyTrades[0].count").value(6))
				.andExpect(jsonPath("$.me.carbonReductionKg").value(41));
	}

	@Test
	void 잘못된_scope_값이면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/carbon-report").param("scope", "DONG"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("scope"));

		then(carbonReportService).should(never()).getReport(any(), any());
	}

}
