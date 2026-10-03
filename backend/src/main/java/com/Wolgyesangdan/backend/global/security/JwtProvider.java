package com.Wolgyesangdan.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

/**
 * 서비스 자체 JWT 발급/검증. subject에 userId를 담는다.
 * type 클레임으로 access/refresh 토큰을 구분해서, 서로의 자리에 쓰지 못하게 막는다.
 * jti(랜덤 id)를 넣어서 같은 사용자에게 같은 초에 발급해도 토큰 문자열이 겹치지 않게 한다.
 */
@Component
public class JwtProvider {

	private static final String TOKEN_TYPE_CLAIM = "type";
	private static final String ACCESS_TOKEN_TYPE = "access";
	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final SecretKey secretKey;
	private final JwtProperties jwtProperties;

	public JwtProvider(JwtProperties jwtProperties) {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
		this.jwtProperties = jwtProperties;
	}

	public String createAccessToken(Long userId) {
		return createToken(userId, ACCESS_TOKEN_TYPE, jwtProperties.accessTokenExpiration());
	}

	public String createRefreshToken(Long userId) {
		return createToken(userId, REFRESH_TOKEN_TYPE, jwtProperties.refreshTokenExpiration());
	}

	/**
	 * access 토큰을 검증하고 userId를 꺼낸다.
	 * 만료면 AUTH_TOKEN_EXPIRED, 그 외 모든 검증 실패는 AUTH_INVALID_TOKEN.
	 */
	public Long getUserIdFromAccessToken(String token) {
		return getUserId(token, ACCESS_TOKEN_TYPE, AuthErrorCode.AUTH_INVALID_TOKEN, AuthErrorCode.AUTH_TOKEN_EXPIRED);
	}

	/**
	 * refresh 토큰을 검증하고 userId를 꺼낸다. (서버 저장소에 남아있는지는 AuthService에서 따로 확인)
	 * 만료면 AUTH_REFRESH_TOKEN_EXPIRED, 그 외 모든 검증 실패는 AUTH_INVALID_REFRESH_TOKEN.
	 */
	public Long getUserIdFromRefreshToken(String token) {
		return getUserId(token, REFRESH_TOKEN_TYPE,
				AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN, AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
	}

	private String createToken(Long userId, String type, Duration expiration) {
		Instant now = Instant.now();
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(userId))
				.claim(TOKEN_TYPE_CLAIM, type)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expiration)))
				.signWith(secretKey)
				.compact();
	}

	private Long getUserId(String token, String expectedType, BaseErrorCode invalidError, BaseErrorCode expiredError) {
		Claims claims;
		try {
			claims = Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (ExpiredJwtException e) {
			throw new BusinessException(expiredError);
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(invalidError);
		}

		if (!expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
			throw new BusinessException(invalidError);
		}
		try {
			return Long.valueOf(claims.getSubject());
		} catch (NumberFormatException e) {
			throw new BusinessException(invalidError);
		}
	}

}
