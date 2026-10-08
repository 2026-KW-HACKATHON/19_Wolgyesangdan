package com.Wolgyesangdan.backend.domain.application.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.application.dto.ApplicationCreateResponse;
import com.Wolgyesangdan.backend.domain.application.dto.MyApplicationSummaryResponse;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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

	@Test
	void 취소하면_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/applications/5").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isNoContent());

		then(applicationService).should().cancel(1L, 5L);
	}

	@Test
	void 본인_신청이_아니면_취소시_403을_반환한다() throws Exception {
		willThrow(new BusinessException(ApplicationErrorCode.APPLICATION_NOT_OWNER))
				.given(applicationService).cancel(1L, 5L);

		mockMvc.perform(delete("/applications/5").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("APPLICATION_NOT_OWNER"));
	}

	@Test
	void 존재하지_않는_신청이면_취소시_404를_반환한다() throws Exception {
		willThrow(new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND))
				.given(applicationService).cancel(1L, 5L);

		mockMvc.perform(delete("/applications/5").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
	}

	@Test
	void 이미_배정된_신청이면_취소시_409를_반환한다() throws Exception {
		willThrow(new BusinessException(ApplicationErrorCode.APPLICATION_ALREADY_SELECTED))
				.given(applicationService).cancel(1L, 5L);

		mockMvc.perform(delete("/applications/5").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_ALREADY_SELECTED"));
	}

	@Test
	void 이미_취소한_신청이면_취소시_409를_반환한다() throws Exception {
		willThrow(new BusinessException(ApplicationErrorCode.APPLICATION_ALREADY_CANCELED))
				.given(applicationService).cancel(1L, 5L);

		mockMvc.perform(delete("/applications/5").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_ALREADY_CANCELED"));
	}

	@Test
	void 토큰_없이_취소하면_401() throws Exception {
		mockMvc.perform(delete("/applications/5"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	void 내가_신청한_물품을_명세의_페이지_형식으로_조회한다() throws Exception {
		given(applicationService.getMyApplications(1L, 0, 20)).willReturn(new PageImpl<>(List.of(
				new MyApplicationSummaryResponse(5L, 1L, "전자레인지", "https://example.com/photo1.jpg",
						ApplicationStatus.WAITING, 2, ItemStatus.ASSIGNED, LocalDateTime.of(2026, 9, 26, 15, 30)),
				new MyApplicationSummaryResponse(3L, 2L, "책상", null, ApplicationStatus.CANCELED, null, ItemStatus.OPEN,
						LocalDateTime.of(2026, 9, 20, 10, 0))),
				PageRequest.of(0, 20), 2));

		getMyApplications("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].id").value(5))
				.andExpect(jsonPath("$.content[0].itemId").value(1))
				.andExpect(jsonPath("$.content[0].itemName").value("전자레인지"))
				.andExpect(jsonPath("$.content[0].itemThumbnailImageUrl").value("https://example.com/photo1.jpg"))
				.andExpect(jsonPath("$.content[0].status").value("WAITING"))
				.andExpect(jsonPath("$.content[0].waitlistRank").value(2))
				.andExpect(jsonPath("$.content[0].itemStatus").value("ASSIGNED"))
				.andExpect(jsonPath("$.content[0].appliedAt").value("2026-09-26T15:30:00"))
				.andExpect(jsonPath("$.content[1].itemThumbnailImageUrl").isEmpty())
				.andExpect(jsonPath("$.content[1].waitlistRank").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.number").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true))
				.andExpect(jsonPath("$.pageable").doesNotExist());
	}

	@Test
	void 내_신청_목록_size는_1에서_100_사이로_보정한다() throws Exception {
		given(applicationService.getMyApplications(1L, 0, 100))
				.willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

		getMyApplications("?page=-1&size=1000")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(100));
	}

	@Test
	void 토큰_없이_내_신청_목록을_조회하면_401() throws Exception {
		mockMvc.perform(get("/users/me/applications"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	private ResultActions getMyApplications(String query) throws Exception {
		return mockMvc.perform(get("/users/me/applications" + query)
				.header(HttpHeaders.AUTHORIZATION, bearer(1L)));
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

}
