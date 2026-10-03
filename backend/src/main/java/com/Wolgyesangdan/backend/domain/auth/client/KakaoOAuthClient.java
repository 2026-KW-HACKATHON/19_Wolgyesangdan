package com.Wolgyesangdan.backend.domain.auth.client;

import java.net.http.HttpClient;
import java.time.Duration;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 인가 코드 → 카카오 액세스 토큰 교환 → 사용자 정보 조회.
 * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api
 */
@Slf4j
@Component
public class KakaoOAuthClient {

	static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
	static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

	private final KakaoProperties kakaoProperties;
	private final RestClient restClient;

	@Autowired
	public KakaoOAuthClient(KakaoProperties kakaoProperties) {
		this(kakaoProperties, RestClient.builder().requestFactory(requestFactoryWithTimeout()));
	}

	// 테스트에서 MockRestServiceServer를 붙인 builder를 넘기기 위한 생성자
	KakaoOAuthClient(KakaoProperties kakaoProperties, RestClient.Builder restClientBuilder) {
		this.kakaoProperties = kakaoProperties;
		this.restClient = restClientBuilder.build();
	}

	public KakaoUser getUser(String authorizationCode) {
		String accessToken = requestAccessToken(authorizationCode);
		return requestUserInfo(accessToken);
	}

	private String requestAccessToken(String authorizationCode) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", kakaoProperties.restApiKey());
		form.add("client_secret", kakaoProperties.clientSecret());
		form.add("redirect_uri", kakaoProperties.redirectUri());
		form.add("code", authorizationCode);

		try {
			KakaoTokenResponse response = restClient.post()
					.uri(TOKEN_URI)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(KakaoTokenResponse.class);
			if (response == null || response.accessToken() == null) {
				throw new BusinessException(AuthErrorCode.AUTH_KAKAO_SERVER_ERROR);
			}
			return response.accessToken();
		} catch (HttpClientErrorException e) {
			// invalid_grant: 인가 코드가 틀렸거나 만료/재사용됨, 또는 redirect_uri 불일치 → 클라이언트 잘못.
			// 그 외(invalid_client 등)는 우리 서버의 키/시크릿 설정 문제라 502로 구분한다.
			String body = e.getResponseBodyAsString();
			if (body.contains("\"invalid_grant\"")) {
				log.warn("카카오 인가 코드 검증 실패: {}", body);
				throw new BusinessException(AuthErrorCode.AUTH_INVALID_KAKAO_CODE);
			}
			log.error("카카오 토큰 발급 실패 (앱 키/시크릿 설정 확인 필요): {}", body);
			throw new BusinessException(AuthErrorCode.AUTH_KAKAO_SERVER_ERROR);
		} catch (RestClientException e) {
			log.error("카카오 토큰 발급 요청 실패", e);
			throw new BusinessException(AuthErrorCode.AUTH_KAKAO_SERVER_ERROR);
		}
	}

	private KakaoUser requestUserInfo(String accessToken) {
		try {
			KakaoUserResponse response = restClient.get()
					.uri(USER_INFO_URI)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.retrieve()
					.body(KakaoUserResponse.class);
			if (response == null || response.id() == null) {
				throw new BusinessException(AuthErrorCode.AUTH_KAKAO_SERVER_ERROR);
			}
			return response.toKakaoUser();
		} catch (RestClientException e) {
			log.error("카카오 사용자 정보 조회 실패", e);
			throw new BusinessException(AuthErrorCode.AUTH_KAKAO_SERVER_ERROR);
		}
	}

	// HttpURLConnection 기반 기본 팩토리는 401 응답 본문을 버려서 카카오 에러 코드(KOE...)를 못 읽는다 → JDK HttpClient 사용
	private static JdkClientHttpRequestFactory requestFactoryWithTimeout() {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(Duration.ofSeconds(5));
		return factory;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

		KakaoUser toKakaoUser() {
			String nickname = kakaoAccount != null && kakaoAccount.profile() != null
					? kakaoAccount.profile().nickname()
					: null;
			String email = kakaoAccount != null ? kakaoAccount.email() : null;
			return new KakaoUser(String.valueOf(id), nickname, email);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record KakaoAccount(Profile profile, String email) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Profile(String nickname) {
	}

}
