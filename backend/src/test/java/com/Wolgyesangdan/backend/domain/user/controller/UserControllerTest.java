package com.Wolgyesangdan.backend.domain.user.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.user.dto.MyInfoResponse;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.service.UserService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.exception.CommonErrorCode;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
	void 토큰은_유효한데_회원이_없으면_404() throws Exception {
		given(userService.getMyInfo(99L)).willThrow(new BusinessException(CommonErrorCode.NOT_FOUND));

		mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(99L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

}
