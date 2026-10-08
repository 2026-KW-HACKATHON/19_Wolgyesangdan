package com.Wolgyesangdan.backend.domain.inquiry.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquiryDetailResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquirySummaryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryAnswerRequest;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;
import com.Wolgyesangdan.backend.domain.inquiry.exception.InquiryErrorCode;
import com.Wolgyesangdan.backend.domain.inquiry.service.AdminInquiryService;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = AdminInquiryController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class AdminInquiryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminInquiryService adminInquiryService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 관리자가_문의_목록을_페이지_형식으로_조회한다() throws Exception {
		given(adminInquiryService.getInquiries(null, PageRequest.of(0, 20))).willReturn(new PageImpl<>(List.of(
				new AdminInquirySummaryResponse(1L, InquiryCategory.HUB, "거점 운영 시간", "문의자",
						LocalDateTime.of(2026, 10, 8, 10, 0, 0), InquiryStatus.OPEN)), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/admin/inquiries").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(1))
				.andExpect(jsonPath("$.content[0].category").value("HUB"))
				.andExpect(jsonPath("$.content[0].title").value("거점 운영 시간"))
				.andExpect(jsonPath("$.content[0].nickname").value("문의자"))
				.andExpect(jsonPath("$.content[0].createdAt").value("2026-10-08T10:00:00"))
				.andExpect(jsonPath("$.content[0].status").value("OPEN"))
				// 목록에는 본문을 내려주지 않는다
				.andExpect(jsonPath("$.content[0].content").doesNotExist())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void status_필터와_page_size를_넘긴다() throws Exception {
		given(adminInquiryService.getInquiries(InquiryStatus.OPEN, PageRequest.of(1, 3)))
				.willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 3), 0));

		mockMvc.perform(get("/admin/inquiries").header(HttpHeaders.AUTHORIZATION, adminToken())
						.param("status", "OPEN").param("page", "1").param("size", "3"))
				.andExpect(status().isOk());

		verify(adminInquiryService).getInquiries(InquiryStatus.OPEN, PageRequest.of(1, 3));
	}

	@Test
	void 없는_상태로_거르면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/admin/inquiries").header(HttpHeaders.AUTHORIZATION, adminToken()).param("status", "DONE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("status"));
	}

	@Test
	void 문의_상세를_조회한다() throws Exception {
		given(adminInquiryService.getInquiry(1L)).willReturn(detail(InquiryStatus.OPEN, null, null));

		mockMvc.perform(get("/admin/inquiries/1").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nickname").value("문의자"))
				.andExpect(jsonPath("$.content").value("주말에도 여나요?"))
				.andExpect(jsonPath("$.answer").isEmpty())
				.andExpect(jsonPath("$.answeredAt").isEmpty());
	}

	@Test
	void 없는_문의면_404_INQUIRY_NOT_FOUND() throws Exception {
		given(adminInquiryService.getInquiry(999L)).willThrow(new BusinessException(InquiryErrorCode.INQUIRY_NOT_FOUND));

		mockMvc.perform(get("/admin/inquiries/999").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INQUIRY_NOT_FOUND"));
	}

	@Test
	void 로그인한_관리자_id로_답변을_등록한다() throws Exception {
		InquiryAnswerRequest request = new InquiryAnswerRequest("주말에는 쉽니다.");
		given(adminInquiryService.answer(1L, 5L, request))
				.willReturn(detail(InquiryStatus.ANSWERED, "주말에는 쉽니다.", LocalDateTime.of(2026, 10, 8, 12, 0, 0)));

		answer(5L, "{\"answer\":\"주말에는 쉽니다.\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ANSWERED"))
				.andExpect(jsonPath("$.answer").value("주말에는 쉽니다."))
				.andExpect(jsonPath("$.answeredAt").value("2026-10-08T12:00:00"));

		verify(adminInquiryService).answer(1L, 5L, request);
	}

	@Test
	void 답변이_비어_있으면_400_INVALID_INPUT() throws Exception {
		answer(5L, "{\"answer\":\" \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("answer"));
	}

	@Test
	void 이미_답변한_문의면_409_INQUIRY_ALREADY_ANSWERED() throws Exception {
		given(adminInquiryService.answer(1L, 5L, new InquiryAnswerRequest("다시 답변")))
				.willThrow(new BusinessException(InquiryErrorCode.INQUIRY_ALREADY_ANSWERED));

		answer(5L, "{\"answer\":\"다시 답변\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INQUIRY_ALREADY_ANSWERED"));
	}

	@Test
	void 비로그인이면_401() throws Exception {
		mockMvc.perform(get("/admin/inquiries"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 일반_회원이면_403_AUTH_FORBIDDEN() throws Exception {
		String userToken = "Bearer " + jwtProvider.createAccessToken(7L);

		mockMvc.perform(get("/admin/inquiries").header(HttpHeaders.AUTHORIZATION, userToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
		mockMvc.perform(get("/admin/inquiries/1").header(HttpHeaders.AUTHORIZATION, userToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/admin/inquiries/1/answer").header(HttpHeaders.AUTHORIZATION, userToken)
						.contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"답변\"}"))
				.andExpect(status().isForbidden());
	}

	private ResultActions answer(Long inquiryId, String body) throws Exception {
		return mockMvc.perform(post("/admin/inquiries/" + inquiryId + "/answer")
				.header(HttpHeaders.AUTHORIZATION, adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String adminToken() {
		return "Bearer " + jwtProvider.createAccessToken(1L, Role.ADMIN);
	}

	private static AdminInquiryDetailResponse detail(InquiryStatus status, String answer, LocalDateTime answeredAt) {
		return new AdminInquiryDetailResponse(1L, InquiryCategory.HUB, "거점 운영 시간", "문의자",
				LocalDateTime.of(2026, 10, 8, 10, 0, 0), status, "주말에도 여나요?", answer, answeredAt);
	}

}
