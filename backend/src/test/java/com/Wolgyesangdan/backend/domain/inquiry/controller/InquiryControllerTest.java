package com.Wolgyesangdan.backend.domain.inquiry.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryCreateRequest;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;
import com.Wolgyesangdan.backend.domain.inquiry.service.InquiryService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
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

@WebMvcTest(controllers = InquiryController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class InquiryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InquiryService inquiryService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 문의를_작성하면_201과_저장된_문의를_내려준다() throws Exception {
		InquiryCreateRequest request = new InquiryCreateRequest(InquiryCategory.HUB, "거점 운영 시간", "주말에도 여나요?");
		given(inquiryService.createInquiry(7L, request)).willReturn(new InquiryResponse(1L, InquiryCategory.HUB,
				"거점 운영 시간", "주말에도 여나요?", InquiryStatus.OPEN, null, null, LocalDateTime.of(2026, 10, 8, 10, 0, 0)));

		create("{\"category\":\"HUB\",\"title\":\"거점 운영 시간\",\"content\":\"주말에도 여나요?\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.category").value("HUB"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.answer").isEmpty())
				.andExpect(jsonPath("$.createdAt").value("2026-10-08T10:00:00"));

		verify(inquiryService).createInquiry(7L, request);
	}

	@Test
	void 카테고리_제목_내용이_빠지면_400_INVALID_INPUT() throws Exception {
		create("{\"title\":\" \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors.length()").value(3));
	}

	@Test
	void 없는_카테고리면_400_INVALID_INPUT과_필드명() throws Exception {
		create("{\"category\":\"REPORT\",\"title\":\"제목\",\"content\":\"내용\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("category"));
	}

	@Test
	void 제목이_100자를_넘으면_400() throws Exception {
		create("{\"category\":\"ETC\",\"title\":\"" + "가".repeat(101) + "\",\"content\":\"내용\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("title"));
	}

	@Test
	void 비로그인으로_문의를_작성하면_401() throws Exception {
		mockMvc.perform(post("/inquiries").contentType(MediaType.APPLICATION_JSON)
						.content("{\"category\":\"ETC\",\"title\":\"제목\",\"content\":\"내용\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 내_문의를_답변과_함께_페이지_형식으로_조회한다() throws Exception {
		given(inquiryService.getMyInquiries(7L, PageRequest.of(0, 20))).willReturn(new PageImpl<>(List.of(
				new InquiryResponse(2L, InquiryCategory.TRADE, "거래 문의", "내용", InquiryStatus.ANSWERED, "답변입니다",
						LocalDateTime.of(2026, 10, 8, 12, 0, 0), LocalDateTime.of(2026, 10, 8, 10, 0, 0))),
				PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/users/me/inquiries").header(HttpHeaders.AUTHORIZATION, token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(2))
				.andExpect(jsonPath("$.content[0].title").value("거래 문의"))
				.andExpect(jsonPath("$.content[0].content").value("내용"))
				.andExpect(jsonPath("$.content[0].status").value("ANSWERED"))
				.andExpect(jsonPath("$.content[0].answer").value("답변입니다"))
				.andExpect(jsonPath("$.content[0].answeredAt").value("2026-10-08T12:00:00"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.size").value(20));
	}

	@Test
	void 비로그인으로_내_문의를_조회하면_401() throws Exception {
		mockMvc.perform(get("/users/me/inquiries"))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions create(String body) throws Exception {
		return mockMvc.perform(post("/inquiries")
				.header(HttpHeaders.AUTHORIZATION, token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String token() {
		return "Bearer " + jwtProvider.createAccessToken(7L);
	}

}
