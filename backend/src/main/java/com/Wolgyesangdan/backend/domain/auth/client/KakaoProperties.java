package com.Wolgyesangdan.backend.domain.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 디벨로퍼스 앱 설정값. redirectUri는 프론트가 인가 요청(Kakao.Auth.authorize)에 쓴 값과 정확히 같아야 한다.
 */
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(String restApiKey, String clientSecret, String redirectUri) {
}
