package com.Wolgyesangdan.backend.domain.campaign.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignCreateRequest;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignListResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignListResponse.PastCampaign;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignUpdateRequest;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.exception.CampaignErrorCode;
import com.Wolgyesangdan.backend.domain.campaign.service.AdminCampaignService;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = AdminCampaignController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class AdminCampaignControllerTest {

	private static final String VALID_CREATE_BODY = """
			{"name":"2026 가을 캠페인","description":"소개",
			 "registrationStartDate":"2026-10-10","registrationEndDate":"2026-10-20",
			 "applicationStartDate":"2026-10-15","applicationEndDate":"2026-10-22",
			 "pickupStartDate":"2026-10-15","pickupEndDate":"2026-10-24",
			 "locationName":"광운대 비마관 1층 거점","locationAddress":"서울 노원구 광운로 20","hubHours":"평일 10:00 ~ 18:00"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminCampaignService adminCampaignService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 현재_캠페인과_지난_캠페인_목록을_조회한다() throws Exception {
		given(adminCampaignService.getCampaigns()).willReturn(new AdminCampaignListResponse(
				campaign(1L, CampaignStatus.ACTIVE),
				List.of(new PastCampaign(2L, "2026 봄 캠페인", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20), 128, 3420))));

		mockMvc.perform(get("/admin/campaigns").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.current.id").value(1))
				.andExpect(jsonPath("$.current.name").value("2026 가을 캠페인"))
				.andExpect(jsonPath("$.current.status").value("ACTIVE"))
				.andExpect(jsonPath("$.current.registrationStartDate").value("2026-10-10"))
				.andExpect(jsonPath("$.current.pickupEndDate").value("2026-10-24"))
				.andExpect(jsonPath("$.current.locationName").value("광운대 비마관 1층 거점"))
				.andExpect(jsonPath("$.current.hubHours").value("평일 10:00 ~ 18:00"))
				.andExpect(jsonPath("$.past[0].id").value(2))
				.andExpect(jsonPath("$.past[0].name").value("2026 봄 캠페인"))
				.andExpect(jsonPath("$.past[0].startDate").value("2026-03-01"))
				.andExpect(jsonPath("$.past[0].endDate").value("2026-03-20"))
				.andExpect(jsonPath("$.past[0].reusedCount").value(128))
				.andExpect(jsonPath("$.past[0].carbonReductionKg").value(3420));
	}

	@Test
	void 현재_캠페인이_없으면_current는_null() throws Exception {
		given(adminCampaignService.getCampaigns()).willReturn(new AdminCampaignListResponse(null, List.of()));

		mockMvc.perform(get("/admin/campaigns").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.current").isEmpty())
				.andExpect(jsonPath("$.past").isEmpty());
	}

	@Test
	void 캠페인을_만들면_201과_Location_헤더로_내려준다() throws Exception {
		given(adminCampaignService.createCampaign(any(AdminCampaignCreateRequest.class)))
				.willReturn(campaign(10L, CampaignStatus.PLANNED));

		create(VALID_CREATE_BODY)
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/admin/campaigns/10"))
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.status").value("PLANNED"));

		verify(adminCampaignService).createCampaign(new AdminCampaignCreateRequest("2026 가을 캠페인", "소개",
				LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20),
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 22),
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 24),
				"광운대 비마관 1층 거점", "서울 노원구 광운로 20", "평일 10:00 ~ 18:00"));
	}

	@Test
	void 이름_날짜_거점이_빠지면_400_INVALID_INPUT() throws Exception {
		create("{\"description\":\"소개\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors.length()").value(9)); // name, 날짜 6개, locationName, locationAddress
	}

	@Test
	void 날짜_형식이_틀리면_400_INVALID_INPUT과_필드명() throws Exception {
		create(VALID_CREATE_BODY.replace("2026-10-24", "10/24"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("pickupEndDate"));
	}

	@Test
	void 예정이거나_진행_중인_캠페인이_있으면_409_CAMPAIGN_ALREADY_RUNNING() throws Exception {
		given(adminCampaignService.createCampaign(any(AdminCampaignCreateRequest.class)))
				.willThrow(new BusinessException(CampaignErrorCode.CAMPAIGN_ALREADY_RUNNING));

		create(VALID_CREATE_BODY)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CAMPAIGN_ALREADY_RUNNING"));
	}

	@Test
	void 기간이_잘못되면_400_CAMPAIGN_PERIOD_INVALID와_어느_기간인지_알려준다() throws Exception {
		given(adminCampaignService.createCampaign(any(AdminCampaignCreateRequest.class)))
				.willThrow(new BusinessException(CampaignErrorCode.CAMPAIGN_PERIOD_INVALID,
						"신청 기간의 시작일은 종료일보다 늦을 수 없습니다."));

		create(VALID_CREATE_BODY)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CAMPAIGN_PERIOD_INVALID"))
				.andExpect(jsonPath("$.message").value("신청 기간의 시작일은 종료일보다 늦을 수 없습니다."));
	}

	@Test
	void 바꿀_필드만_보내서_수정한다() throws Exception {
		given(adminCampaignService.updateCampaign(eq(1L), any(AdminCampaignUpdateRequest.class)))
				.willReturn(campaign(1L, CampaignStatus.ENDED));

		update(1L, "{\"name\":\"새 이름\",\"status\":\"ENDED\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("ENDED"));

		verify(adminCampaignService).updateCampaign(1L, new AdminCampaignUpdateRequest("새 이름", null, null, null, null,
				null, null, null, null, null, null, CampaignStatus.ENDED));
	}

	@Test
	void status에_PLANNED를_보내면_400() throws Exception {
		update(1L, "{\"status\":\"PLANNED\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("statusToggle"));
	}

	@Test
	void 이름이나_거점을_빈_값으로_바꾸려_하면_400() throws Exception {
		update(1L, "{\"name\":\" \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("name"));
		update(1L, "{\"locationName\":\"\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("locationName"));
	}

	@Test
	void 없는_캠페인이면_404_CAMPAIGN_NOT_FOUND() throws Exception {
		given(adminCampaignService.updateCampaign(eq(999L), any(AdminCampaignUpdateRequest.class)))
				.willThrow(new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));

		update(999L, "{\"name\":\"새 이름\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CAMPAIGN_NOT_FOUND"));
	}

	@Test
	void 비로그인이면_401() throws Exception {
		mockMvc.perform(get("/admin/campaigns"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 일반_회원이면_403_AUTH_FORBIDDEN() throws Exception {
		String userToken = "Bearer " + jwtProvider.createAccessToken(7L);

		mockMvc.perform(get("/admin/campaigns").header(HttpHeaders.AUTHORIZATION, userToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
		mockMvc.perform(post("/admin/campaigns").header(HttpHeaders.AUTHORIZATION, userToken)
						.contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE_BODY))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/admin/campaigns/1").header(HttpHeaders.AUTHORIZATION, userToken)
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ENDED\"}"))
				.andExpect(status().isForbidden());
	}

	private ResultActions create(String body) throws Exception {
		return mockMvc.perform(post("/admin/campaigns")
				.header(HttpHeaders.AUTHORIZATION, adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions update(Long campaignId, String body) throws Exception {
		return mockMvc.perform(patch("/admin/campaigns/" + campaignId)
				.header(HttpHeaders.AUTHORIZATION, adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String adminToken() {
		return "Bearer " + jwtProvider.createAccessToken(1L, Role.ADMIN);
	}

	private static AdminCampaignResponse campaign(Long id, CampaignStatus status) {
		return new AdminCampaignResponse(id, "2026 가을 캠페인", "소개", status,
				LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20),
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 22),
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 24),
				"광운대 비마관 1층 거점", "서울 노원구 광운로 20", "평일 10:00 ~ 18:00");
	}

}
