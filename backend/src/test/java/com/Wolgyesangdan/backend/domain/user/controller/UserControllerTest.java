package com.Wolgyesangdan.backend.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.user.dto.ContactResponse;
import com.Wolgyesangdan.backend.domain.user.dto.ContactUpdateRequest;
import com.Wolgyesangdan.backend.domain.user.dto.MyInfoResponse;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.service.UserService;
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

@WebMvcTest(controllers = UserController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private UserService userService;

	@Test
	void 로그인한_사용자의_정보를_조회한다() throws Exception {
		given(userService.getMyInfo(1L)).willReturn(new MyInfoResponse(1L, "월계1동 이웃", "user@example.com",
				ContactType.OPENCHAT, null, "https://open.kakao.com/o/xxxxxxx",
				LocalDateTime.of(2026, 9, 10, 12, 0)));

		mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nickname").value("월계1동 이웃"))
				.andExpect(jsonPath("$.email").value("user@example.com"))
				.andExpect(jsonPath("$.contactType").value("OPENCHAT"))
				.andExpect(jsonPath("$.phone").isEmpty())
				.andExpect(jsonPath("$.openchatLink").value("https://open.kakao.com/o/xxxxxxx"))
				.andExpect(jsonPath("$.createdAt").value("2026-09-10T12:00:00"));
	}

	@Test
	void 연락_수단을_설정하지_않았으면_null로_내려준다() throws Exception {
		given(userService.getMyInfo(2L)).willReturn(new MyInfoResponse(2L, "카카오사용자1234", null, null, null, null,
				LocalDateTime.of(2026, 9, 10, 12, 0)));

		mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(2L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").isEmpty())
				.andExpect(jsonPath("$.contactType").isEmpty())
				.andExpect(jsonPath("$.phone").isEmpty())
				.andExpect(jsonPath("$.openchatLink").isEmpty());
	}

	@Test
	void 토큰_없이_조회하면_401() throws Exception {
		mockMvc.perform(get("/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	void 토큰은_유효한데_회원이_없으면_401_AUTH_USER_NOT_FOUND() throws Exception {
		given(userService.getMyInfo(99L)).willThrow(new BusinessException(AuthErrorCode.AUTH_USER_NOT_FOUND));

		mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(99L)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_USER_NOT_FOUND"));
	}

	@Test
	void 내_연락_수단을_조회한다() throws Exception {
		given(userService.getMyContact(1L)).willReturn(
				new ContactResponse(ContactType.PHONE, "010-1234-5678", "https://open.kakao.com/o/xxxxxxx"));

		mockMvc.perform(get("/users/me/contact").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactType").value("PHONE"))
				.andExpect(jsonPath("$.phone").value("010-1234-5678"))
				.andExpect(jsonPath("$.openchatLink").value("https://open.kakao.com/o/xxxxxxx"));
	}

	@Test
	void 연락_수단_미설정이면_모두_null() throws Exception {
		given(userService.getMyContact(2L)).willReturn(new ContactResponse(null, null, null));

		mockMvc.perform(get("/users/me/contact").header(HttpHeaders.AUTHORIZATION, bearer(2L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactType").isEmpty())
				.andExpect(jsonPath("$.phone").isEmpty())
				.andExpect(jsonPath("$.openchatLink").isEmpty());
	}

	@Test
	void 토큰_없이_연락_수단을_조회하면_401() throws Exception {
		mockMvc.perform(get("/users/me/contact"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	void 오픈채팅으로_연락_수단을_설정한다() throws Exception {
		given(userService.updateMyContact(eq(1L), any(ContactUpdateRequest.class))).willReturn(
				new ContactResponse(ContactType.OPENCHAT, null, "https://open.kakao.com/o/abc123"));

		putContact(1L, "{\"contactType\":\"OPENCHAT\",\"openchatLink\":\"https://open.kakao.com/o/abc123\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactType").value("OPENCHAT"))
				.andExpect(jsonPath("$.openchatLink").value("https://open.kakao.com/o/abc123"));
	}

	@Test
	void 전화번호로_연락_수단을_설정한다() throws Exception {
		given(userService.updateMyContact(eq(1L), any(ContactUpdateRequest.class))).willReturn(
				new ContactResponse(ContactType.PHONE, "010-1234-5678", null));

		putContact(1L, "{\"contactType\":\"PHONE\",\"phone\":\"010-1234-5678\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactType").value("PHONE"))
				.andExpect(jsonPath("$.phone").value("010-1234-5678"));
	}

	@Test
	void contactType이_없으면_400() throws Exception {
		putContact(1L, "{\"phone\":\"010-1234-5678\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("contactType"));
	}

	@Test
	void PHONE인데_전화번호가_없으면_400() throws Exception {
		putContact(1L, "{\"contactType\":\"PHONE\",\"openchatLink\":\"https://open.kakao.com/o/abc123\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("valueProvidedForContactType"));
	}

	@Test
	void 전화번호_형식이_틀리면_400() throws Exception {
		putContact(1L, "{\"contactType\":\"PHONE\",\"phone\":\"01012345678\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("phone"));
	}

	@Test
	void 오픈채팅_링크_형식이_틀리면_400() throws Exception {
		putContact(1L, "{\"contactType\":\"OPENCHAT\",\"openchatLink\":\"https://example.com/o/abc\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("openchatLink"));
	}

	@Test
	void 토큰_없이_연락_수단을_설정하면_401() throws Exception {
		mockMvc.perform(put("/users/me/contact")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contactType\":\"PHONE\",\"phone\":\"010-1234-5678\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	private ResultActions putContact(Long userId, String body) throws Exception {
		return mockMvc.perform(put("/users/me/contact")
				.header(HttpHeaders.AUTHORIZATION, bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

}
