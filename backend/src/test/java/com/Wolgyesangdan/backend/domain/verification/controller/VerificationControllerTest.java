package com.Wolgyesangdan.backend.domain.verification.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.DocumentType;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationDocumentUploadService;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;
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

@WebMvcTest(controllers = VerificationController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class VerificationControllerTest {

	private static final String FILE_KEY = "verifications/2026/10/08/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.jpg";
	/** 신청 본문의 서류 파일·실명 부분 */
	private static final String DOCUMENT = ",\"fileKey\":\"" + FILE_KEY + "\",\"applicantName\":\"김하늘\"";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private VerificationService verificationService;

	@MockitoBean
	private VerificationDocumentUploadService verificationDocumentUploadService;

	@Test
	void 내_인증_상태를_유형별로_조회한다() throws Exception {
		given(verificationService.getMyVerifications(1L)).willReturn(List.of(
				new MyVerificationResponse(VerificationType.NEIGHBORHOOD, null, VerificationStatus.APPROVED,
						LocalDateTime.of(2026, 10, 5, 9, 0), null, null, null),
				new MyVerificationResponse(VerificationType.FRESHMAN, DocumentType.ADMISSION_LETTER,
						VerificationStatus.APPROVED, LocalDateTime.of(2026, 9, 10, 9, 0),
						LocalDateTime.of(2026, 9, 11, 10, 0), null, LocalDateTime.of(2027, 2, 28, 23, 59, 59)),
				new MyVerificationResponse(VerificationType.LOW_INCOME, DocumentType.RECIPIENT_CERTIFICATE,
						VerificationStatus.REJECTED, LocalDateTime.of(2026, 9, 12, 9, 0),
						LocalDateTime.of(2026, 9, 13, 10, 0), "제출된 서류에서 대상 여부를 확인할 수 없어요.", null)));

		mockMvc.perform(get("/verifications/me").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].verificationType").value("NEIGHBORHOOD"))
				.andExpect(jsonPath("$[0].documentType").isEmpty())
				.andExpect(jsonPath("$[0].status").value("APPROVED"))
				.andExpect(jsonPath("$[1].verificationType").value("FRESHMAN"))
				.andExpect(jsonPath("$[1].documentType").value("ADMISSION_LETTER"))
				.andExpect(jsonPath("$[1].status").value("APPROVED"))
				.andExpect(jsonPath("$[1].submittedAt").value("2026-09-10T09:00:00"))
				.andExpect(jsonPath("$[1].reviewedAt").value("2026-09-11T10:00:00"))
				.andExpect(jsonPath("$[1].rejectionReason").isEmpty())
				.andExpect(jsonPath("$[1].expiresAt").value("2027-02-28T23:59:59"))
				.andExpect(jsonPath("$[2].verificationType").value("LOW_INCOME"))
				.andExpect(jsonPath("$[2].documentType").value("RECIPIENT_CERTIFICATE"))
				.andExpect(jsonPath("$[2].status").value("REJECTED"))
				.andExpect(jsonPath("$[2].rejectionReason").value("제출된 서류에서 대상 여부를 확인할 수 없어요."))
				.andExpect(jsonPath("$[2].expiresAt").isEmpty());
	}

	@Test
	void 심사_중이면_심사_관련_값은_null로_내려준다() throws Exception {
		given(verificationService.getMyVerifications(1L)).willReturn(List.of(
				new MyVerificationResponse(VerificationType.FRESHMAN, DocumentType.STUDENT_ID_CARD,
						VerificationStatus.PENDING, LocalDateTime.of(2026, 9, 26, 15, 0), null, null, null)));

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

	@Test
	void 신입생_인증을_신청한다() throws Exception {
		given(verificationService.createVerification(eq(1L), any(VerificationCreateRequest.class))).willReturn(
				new VerificationCreateResponse(10L, VerificationType.FRESHMAN, VerificationStatus.PENDING,
						LocalDateTime.of(2026, 9, 26, 15, 0)));

		postVerification(1L, "{\"verificationType\":\"FRESHMAN\",\"documentType\":\"ADMISSION_LETTER\"" + DOCUMENT + "}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.verificationType").value("FRESHMAN"))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.submittedAt").value("2026-09-26T15:00:00"));
	}

	@Test
	void 기초수급자_인증을_신청한다() throws Exception {
		given(verificationService.createVerification(eq(1L), any(VerificationCreateRequest.class))).willReturn(
				new VerificationCreateResponse(12L, VerificationType.LOW_INCOME, VerificationStatus.PENDING,
						LocalDateTime.of(2026, 9, 26, 15, 0)));

		postVerification(1L, "{\"verificationType\":\"LOW_INCOME\",\"documentType\":\"RECIPIENT_CERTIFICATE\"" + DOCUMENT + "}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.verificationType").value("LOW_INCOME"));
	}

	@Test
	void verificationType이_없으면_400() throws Exception {
		postVerification(1L, "{\"documentType\":\"ADMISSION_LETTER\"" + DOCUMENT + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("verificationType"));
	}

	@Test
	void documentType이_없으면_400() throws Exception {
		postVerification(1L, "{\"verificationType\":\"FRESHMAN\"" + DOCUMENT + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("documentType"));
	}

	@Test
	void 유형에_맞지_않는_서류_종류면_400() throws Exception {
		postVerification(1L, "{\"verificationType\":\"LOW_INCOME\",\"documentType\":\"STUDENT_ID_CARD\"" + DOCUMENT + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("documentTypeMatchingVerificationType"));
	}

	@Test
	void 동네_인증은_여기서_신청할_수_없다() throws Exception {
		postVerification(1L, "{\"verificationType\":\"NEIGHBORHOOD\",\"documentType\":\"ADMISSION_LETTER\"" + DOCUMENT + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("priorityVerificationType")));
	}

	@Test
	void 없어진_예전_유형이면_400() throws Exception {
		postVerification(1L, "{\"verificationType\":\"STUDENT\",\"documentType\":\"STUDENT_ID_CARD\"" + DOCUMENT + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("verificationType"));
	}

	@Test
	void 이미_심사_중인_신청이_있으면_409() throws Exception {
		given(verificationService.createVerification(eq(1L), any(VerificationCreateRequest.class)))
				.willThrow(new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_PENDING));

		postVerification(1L, "{\"verificationType\":\"LOW_INCOME\",\"documentType\":\"RECIPIENT_CERTIFICATE\"" + DOCUMENT + "}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("VERIFICATION_ALREADY_PENDING"))
				.andExpect(jsonPath("$.message").value("이미 심사 중인 인증 신청이 있습니다."))
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	@Test
	void 이미_승인된_인증이_있으면_409() throws Exception {
		given(verificationService.createVerification(eq(1L), any(VerificationCreateRequest.class)))
				.willThrow(new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED));

		postVerification(1L, "{\"verificationType\":\"LOW_INCOME\",\"documentType\":\"RECIPIENT_CERTIFICATE\"" + DOCUMENT + "}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VERIFICATION_ALREADY_APPROVED"))
				.andExpect(jsonPath("$.message").value("이미 승인된 인증이 있습니다."));
	}

	@Test
	void 토큰_없이_신청하면_401() throws Exception {
		mockMvc.perform(post("/verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"verificationType\":\"LOW_INCOME\",\"documentType\":\"RECIPIENT_CERTIFICATE\"" + DOCUMENT + "}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	void 동네_인증을_한다() throws Exception {
		given(verificationService.verifyNeighborhood(1L)).willReturn(
				new VerificationCreateResponse(20L, VerificationType.NEIGHBORHOOD, VerificationStatus.APPROVED,
						LocalDateTime.of(2026, 10, 5, 9, 0)));

		mockMvc.perform(post("/verifications/neighborhood").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(20))
				.andExpect(jsonPath("$.verificationType").value("NEIGHBORHOOD"))
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.submittedAt").value("2026-10-05T09:00:00"));
	}

	@Test
	void 동네_인증이_이미_있으면_409() throws Exception {
		given(verificationService.verifyNeighborhood(1L))
				.willThrow(new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED));

		mockMvc.perform(post("/verifications/neighborhood").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VERIFICATION_ALREADY_APPROVED"));
	}

	@Test
	void 토큰_없이_동네_인증하면_401() throws Exception {
		mockMvc.perform(post("/verifications/neighborhood"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	void 서류_파일_없이_신청하면_400() throws Exception {
		postVerification(1L, "{\"verificationType\":\"FRESHMAN\",\"documentType\":\"ADMISSION_LETTER\","
				+ "\"applicantName\":\"김하늘\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("fileKey"));
	}

	@Test
	void 서버가_발급한_서류_경로가_아니면_400() throws Exception {
		for (String fileKey : new String[] {"items/2026/10/08/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.jpg",
				"verifications/../items/x.jpg", "https://evil.example.com/a.jpg"}) {
			postVerification(1L, "{\"verificationType\":\"FRESHMAN\",\"documentType\":\"ADMISSION_LETTER\","
					+ "\"fileKey\":\"" + fileKey + "\",\"applicantName\":\"김하늘\"}")
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors[0].field").value("fileKey"))
					.andExpect(jsonPath("$.errors[0].message").value("올바른 서류 파일이 아닙니다."));
		}
	}

	@Test
	void 실명_없이_신청하면_400() throws Exception {
		postVerification(1L, "{\"verificationType\":\"FRESHMAN\",\"documentType\":\"ADMISSION_LETTER\","
				+ "\"fileKey\":\"" + FILE_KEY + "\",\"applicantName\":\" \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("applicantName"));
	}

	@Test
	void 서류_업로드_URL을_발급한다() throws Exception {
		given(verificationDocumentUploadService.issueUploadUrl(any(DocumentUploadUrlRequest.class))).willReturn(
				new DocumentUploadUrlResponse("https://s3.example.com/" + FILE_KEY + "?sig=1", FILE_KEY));

		postUploadUrl("{\"fileName\":\"합격증.pdf\",\"contentType\":\"application/pdf\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploadUrl").value("https://s3.example.com/" + FILE_KEY + "?sig=1"))
				.andExpect(jsonPath("$.fileKey").value(FILE_KEY))
				.andExpect(jsonPath("$.imageUrl").doesNotExist());
	}

	@Test
	void 지원하지_않는_서류_형식이면_400() throws Exception {
		postUploadUrl("{\"fileName\":\"a.gif\",\"contentType\":\"image/gif\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("contentType"));
	}

	@Test
	void S3가_설정되지_않았으면_서류_업로드_URL은_503() throws Exception {
		given(verificationDocumentUploadService.issueUploadUrl(any(DocumentUploadUrlRequest.class)))
				.willThrow(new BusinessException(VerificationErrorCode.VERIFICATION_DOCUMENT_UPLOAD_UNAVAILABLE));

		postUploadUrl("{\"fileName\":\"a.jpg\",\"contentType\":\"image/jpeg\"}")
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("VERIFICATION_DOCUMENT_UPLOAD_UNAVAILABLE"));
	}

	@Test
	void 토큰_없이_서류_업로드_URL을_요청하면_401() throws Exception {
		mockMvc.perform(post("/verifications/documents/upload-url")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fileName\":\"a.jpg\",\"contentType\":\"image/jpeg\"}"))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions postUploadUrl(String body) throws Exception {
		return mockMvc.perform(post("/verifications/documents/upload-url")
				.header(HttpHeaders.AUTHORIZATION, bearer(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions postVerification(Long userId, String body) throws Exception {
		return mockMvc.perform(post("/verifications")
				.header(HttpHeaders.AUTHORIZATION, bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

}
