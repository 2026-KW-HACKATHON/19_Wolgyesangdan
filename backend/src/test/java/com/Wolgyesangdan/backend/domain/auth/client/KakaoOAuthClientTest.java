package com.Wolgyesangdan.backend.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoOAuthClientTest {

	private static final String TOKEN_RESPONSE = """
			{"token_type":"bearer","access_token":"kakao-access","expires_in":21599,"refresh_token":"r","scope":"profile_nickname"}
			""";

	private MockRestServiceServer server;
	private KakaoOAuthClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new KakaoOAuthClient(
				new KakaoProperties("rest-key", "secret", "http://localhost:5173/oauth/kakao/callback"), builder);
	}

	@Test
	void 인가_코드로_토큰을_교환하고_사용자_정보를_가져온다() {
		server.expect(requestTo(KakaoOAuthClient.TOKEN_URI))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().formDataContains(java.util.Map.of(
						"grant_type", "authorization_code",
						"client_id", "rest-key",
						"client_secret", "secret",
						"redirect_uri", "http://localhost:5173/oauth/kakao/callback",
						"code", "auth-code")))
				.andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
		server.expect(requestTo(KakaoOAuthClient.USER_INFO_URI))
				.andExpect(header("Authorization", "Bearer kakao-access"))
				.andRespond(withSuccess("""
						{"id":4012345678,"connected_at":"2026-10-04T00:00:00Z",
						 "kakao_account":{"profile":{"nickname":"용민","is_default_nickname":false},"email":"a@b.com"}}
						""", MediaType.APPLICATION_JSON));

		KakaoUser user = client.getUser("auth-code");

		assertThat(user).isEqualTo(new KakaoUser("4012345678", "용민", "a@b.com"));
		server.verify();
	}

	@Test
	void 동의하지_않은_항목은_null로_받는다() {
		server.expect(requestTo(KakaoOAuthClient.TOKEN_URI))
				.andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
		server.expect(requestTo(KakaoOAuthClient.USER_INFO_URI))
				.andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

		assertThat(client.getUser("auth-code")).isEqualTo(new KakaoUser("1", null, null));
	}

	@Test
	void 인가_코드가_잘못되면_AUTH_INVALID_KAKAO_CODE() {
		server.expect(requestTo(KakaoOAuthClient.TOKEN_URI))
				.andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON).body("""
						{"error":"invalid_grant","error_description":"authorization code not found for code=x","error_code":"KOE320"}
						"""));

		assertThatThrownBy(() -> client.getUser("x"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_INVALID_KAKAO_CODE);
	}

	@Test
	void 앱_키_설정이_잘못되면_AUTH_KAKAO_SERVER_ERROR() {
		server.expect(requestTo(KakaoOAuthClient.TOKEN_URI))
				.andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON).body("""
						{"error":"invalid_client","error_description":"Bad client credentials","error_code":"KOE010"}
						"""));

		assertThatThrownBy(() -> client.getUser("x"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_KAKAO_SERVER_ERROR);
	}

	@Test
	void 카카오_서버_오류면_AUTH_KAKAO_SERVER_ERROR() {
		server.expect(requestTo(KakaoOAuthClient.TOKEN_URI)).andRespond(withServerError());

		assertThatThrownBy(() -> client.getUser("x"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_KAKAO_SERVER_ERROR);
	}

}
