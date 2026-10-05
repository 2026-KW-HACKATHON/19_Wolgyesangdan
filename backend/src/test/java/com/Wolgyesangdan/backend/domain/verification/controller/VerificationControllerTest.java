package com.Wolgyesangdan.backend.domain.verification.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;
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

@WebMvcTest(controllers = VerificationController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class VerificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private VerificationService verificationService;

	@Test
	void 내_인증_상태를_유형별로_조회한다() throws Exception {
		given(verificationService.getMyVerifications(1L)).willReturn(List.of(
				new MyVerificationResponse(VerificationType.FRESHMAN, VerificationStatus.APPROVED,
						LocalDateTime.of(2026, 9, 10, 9, 0), LocalDateTime.of(2026, 9, 11, 10, 0), null,
						LocalDateTime.of(2027, 2, 28, 23, 59, 59)),
				new MyVerificationResponse(VerificationType.LOW_INCOME, VerificationStatus.REJECTED,
						LocalDateTime.of(2026, 9, 12, 9, 0), LocalDateTime.of(2026, 9, 13, 10, 0),
						"제출된 서류에서 대상 여부를 확인할 수 없어요.", null)));

		mockMvc.perform(get("/verifications/me").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].verificationType").value("FRESHMAN"))
				.andExpect(jsonPath("$[0].status").value("APPROVED"))
				.andExpect(jsonPath("$[0].submittedAt").value("2026-09-10T09:00:00"))
				.andExpect(jsonPath("$[0].reviewedAt").value("2026-09-11T10:00:00"))
				.andExpect(jsonPath("$[0].rejectionReason").isEmpty())
				.andExpect(jsonPath("$[0].expiresAt").value("2027-02-28T23:59:59"))
				.andExpect(jsonPath("$[1].verificationType").value("LOW_INCOME"))
				.andExpect(jsonPath("$[1].status").value("REJECTED"))
				.andExpect(jsonPath("$[1].rejectionReason").value("제출된 서류에서 대상 여부를 확인할 수 없어요."))
				.andExpect(jsonPath("$[1].expiresAt").isEmpty());
	}

	@Test
	void 심사_중이면_심사_관련_값은_null로_내려준다() throws Exception {
		given(verificationService.getMyVerifications(1L)).willReturn(List.of(
				new MyVerificationResponse(VerificationType.RESIDENT, VerificationStatus.PENDING,
						LocalDateTime.of(2026, 9, 26, 15, 0), null, null, null)));

		mockMvc.perform(get("/verifications/me").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("PENDING"))
				.andExpect(jsonPath("$[0].reviewedAt").isEmpty())
				.andExpect(jsonPath("$[0].rejectionReason").isEmpty())
				.andExpect(jsonPath("$[0].expiresAt").isEmpty());
	}

	@Test
	void 신청한_적_없으면_빈_배열() throws Exception {
		given(verificationService.getMyVerifications(1L)).willReturn(List.of());

		mockMvc.perform(get("/verifications/me").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void 토큰_없이_조회하면_401() throws Exception {
		mockMvc.perform(get("/verifications/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

}
