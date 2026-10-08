package com.Wolgyesangdan.backend.domain.verification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationApproveRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationDetailResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationFileResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationSummaryResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.domain.verification.service.AdminVerificationService;
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

@WebMvcTest(controllers = AdminVerificationController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class AdminVerificationControllerTest {

	private static final Long ADMIN_ID = 9L;
	private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 10, 5, 9, 0);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminVerificationService adminVerificationService;

	@Test
	void 검토_대기_서류_목록을_페이지_형식으로_조회한다() throws Exception {
		given(adminVerificationService.getVerifications(VerificationStatus.PENDING, PageRequest.of(0, 20)))
				.willReturn(new PageImpl<>(List.of(new AdminVerificationSummaryResponse(10L, "김하늘", "하늘",
						VerificationType.FRESHMAN, DocumentType.ADMISSION_LETTER, SUBMITTED_AT,
						VerificationStatus.PENDING)), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/admin/verifications").param("status", "PENDING").header(HttpHeaders.AUTHORIZATION, admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(10))
				.andExpect(jsonPath("$.content[0].applicantName").value("김하늘"))
				.andExpect(jsonPath("$.content[0].nickname").value("하늘"))
				.andExpect(jsonPath("$.content[0].verificationType").value("FRESHMAN"))
				.andExpect(jsonPath("$.content[0].documentType").value("ADMISSION_LETTER"))
				.andExpect(jsonPath("$.content[0].status").value("PENDING"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void 서류_상세를_조회한다() throws Exception {
		given(adminVerificationService.getVerification(10L)).willReturn(detail(VerificationStatus.PENDING, null));

		mockMvc.perform(get("/admin/verifications/10").header(HttpHeaders.AUTHORIZATION, admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.neighborhoodVerified").value(true))
				.andExpect(jsonPath("$.hasDocument").value(true))
				.andExpect(jsonPath("$.reviewerNickname").isEmpty());
	}

	@Test
	void 서류_열람_URL을_받는다() throws Exception {
		given(adminVerificationService.issueFileUrl(ADMIN_ID, 10L)).willReturn(new AdminVerificationFileResponse(
				"https://signed.example.com/doc?sig=1", "application/pdf", LocalDateTime.of(2026, 10, 8, 14, 5)));

		mockMvc.perform(get("/admin/verifications/10/file").header(HttpHeaders.AUTHORIZATION, admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://signed.example.com/doc?sig=1"))
				.andExpect(jsonPath("$.contentType").value("application/pdf"))
				.andExpect(jsonPath("$.expiresAt").value("2026-10-08T14:05:00"));
	}

	@Test
	void 신입생을_입학_연도와_함께_승인한다() throws Exception {
		given(adminVerificationService.approve(eq(ADMIN_ID), eq(10L), any(AdminVerificationApproveRequest.class)))
				.willReturn(detail(VerificationStatus.APPROVED, "운영자"));

		mockMvc.perform(post("/admin/verifications/10/approve").header(HttpHeaders.AUTHORIZATION, admin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"admissionYear\":2027}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.reviewerNickname").value("운영자"));
		verify(adminVerificationService).approve(ADMIN_ID, 10L, new AdminVerificationApproveRequest(2027));
	}

	@Test
	void 기초수급자는_본문_없이_승인한다() throws Exception {
		given(adminVerificationService.approve(eq(ADMIN_ID), eq(10L), isNull()))
				.willReturn(detail(VerificationStatus.APPROVED, "운영자"));

		mockMvc.perform(post("/admin/verifications/10/approve").header(HttpHeaders.AUTHORIZATION, admin()))
				.andExpect(status().isOk());
		verify(adminVerificationService).approve(ADMIN_ID, 10L, null);
	}

	@Test
	void 반려_사유가_없으면_400() throws Exception {
		mockMvc.perform(post("/admin/verifications/10/reject").header(HttpHeaders.AUTHORIZATION, admin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("reason"));
	}

	@Test
	void 이미_처리된_신청이면_409() throws Exception {
		given(adminVerificationService.reject(ADMIN_ID, 10L, "흐려요"))
				.willThrow(new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_REVIEWED));

		mockMvc.perform(post("/admin/verifications/10/reject").header(HttpHeaders.AUTHORIZATION, admin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"흐려요\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VERIFICATION_ALREADY_REVIEWED"));
	}

	@Test
	void 일반_회원은_403_비로그인은_401() throws Exception {
		mockMvc.perform(get("/admin/verifications")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
		mockMvc.perform(get("/admin/verifications/10/file"))
				.andExpect(status().isUnauthorized());
	}

	private AdminVerificationDetailResponse detail(VerificationStatus status, String reviewer) {
		return new AdminVerificationDetailResponse(10L, "김하늘", "하늘", VerificationType.FRESHMAN,
				DocumentType.ADMISSION_LETTER, SUBMITTED_AT, status, true, true, null,
				reviewer == null ? null : LocalDateTime.of(2026, 10, 8, 14, 0), reviewer,
				status == VerificationStatus.APPROVED ? LocalDateTime.of(2027, 12, 31, 23, 59, 59) : null);
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(ADMIN_ID, Role.ADMIN);
	}

}
