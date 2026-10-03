package com.Wolgyesangdan.backend.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import com.Wolgyesangdan.backend.global.config.SecurityConfig;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class,
		properties = "jwt.secret=" + SecurityConfigTest.SECRET)
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class,
		SecurityConfigTest.TestController.class})
class SecurityConfigTest {

	static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 유효한_토큰이면_principal로_userId가_들어온다() throws Exception {
		mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(7L)))
				.andExpect(status().isOk())
				.andExpect(content().string("7"));
	}

	@Test
	void 토큰_없이_인증_필요_API를_호출하면_401_AUTH_UNAUTHORIZED() throws Exception {
		mockMvc.perform(get("/test/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	void 위조된_토큰이면_401_AUTH_INVALID_TOKEN() throws Exception {
		mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void 다른_키로_서명된_토큰이면_401_AUTH_INVALID_TOKEN() throws Exception {
		String token = Jwts.builder()
				.subject("7")
				.claim("type", "access")
				.signWith(Keys.hmacShaKeyFor("another-secret-key-that-is-long-enough-hs256".getBytes(StandardCharsets.UTF_8)))
				.compact();

		mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void access_타입이_아닌_토큰이면_401_AUTH_INVALID_TOKEN() throws Exception {
		mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken("refresh", Instant.now().plusSeconds(60))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void 만료된_토큰이면_401_AUTH_TOKEN_EXPIRED() throws Exception {
		mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken("access", Instant.now().minusSeconds(60))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
	}

	@Test
	void 비회원_허용_API는_토큰_없이_호출할_수_있고_principal은_null() throws Exception {
		mockMvc.perform(get("/items"))
				.andExpect(status().isOk())
				.andExpect(content().string("anonymous"));
	}

	@Test
	void 비회원_허용_API는_토큰이_잘못돼도_비회원으로_통과한다() throws Exception {
		mockMvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
				.andExpect(status().isOk())
				.andExpect(content().string("anonymous"));
	}

	private String signedToken(String type, Instant expiration) {
		return Jwts.builder()
				.subject("7")
				.claim("type", type)
				.expiration(Date.from(expiration))
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
				.compact();
	}

	@RestController
	static class TestController {

		@GetMapping("/test/me")
		String me(@AuthenticationPrincipal Long userId) {
			return String.valueOf(userId);
		}

		@GetMapping("/items")
		String items(@AuthenticationPrincipal Long userId) {
			return userId == null ? "anonymous" : String.valueOf(userId);
		}

	}

}
