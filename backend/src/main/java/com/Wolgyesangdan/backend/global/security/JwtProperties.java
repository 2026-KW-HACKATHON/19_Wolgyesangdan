package com.Wolgyesangdan.backend.global.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 jwt.* 설정. secret은 HS256 서명용이라 32바이트 이상이어야 한다 (짧으면 기동 시 실패).
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration accessTokenExpiration, Duration refreshTokenExpiration) {
}
