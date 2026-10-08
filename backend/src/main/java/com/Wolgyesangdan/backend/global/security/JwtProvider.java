package com.Wolgyesangdan.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

/**
 * 서비스 자체 JWT 발급/검증. subject에 userId를 담는다.
 * type 클레임으로 access/refresh 토큰을 구분해서, 서로의 자리에 쓰지 못하게 막는다.
 * jti(랜덤 id)를 넣어서 같은 사용자에게 같은 초에 발급해도 토큰 문자열이 겹치지 않게 한다.
 * access 토큰에는 role 클레임도 담는다 — 관리자 API 권한 확인용. 권한을 바꾸면 다음 토큰 재발급(최대 access 만료 시간) 때 반영된다.
 */
@Component
public class JwtProvider {

	private static final String TOKEN_TYPE_CLAIM = "type";
	private static final String ROLE_CLAIM = "role";
	private static final String ACCESS_TOKEN_TYPE = "access";
	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final SecretKey secretKey;
	private final JwtProperties jwtProperties;

	public JwtProvider(JwtProperties jwtProperties) {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
		this.jwtProperties = jwtProperties;
	}

	/** 일반 회원(USER) access 토큰 */
	public String createAccessToken(Long userId) {
		return createAccessToken(userId, Role.USER);
	}

	public String createAccessToken(Long userId, Role role) {
		return tokenBuilder(userId, ACCESS_TOKEN_TYPE, jwtProperties.accessTokenExpiration())
				.claim(ROLE_CLAIM, role.name())
				.compact();
	}

	public String createRefreshToken(Long userId) {
		return tokenBuilder(userId, REFRESH_TOKEN_TYPE, jwtProperties.refreshTokenExpiration()).compact();
	}

	/**
	 * access 토큰을 검증하고 userId를 꺼낸다.
	 * 만료면 AUTH_TOKEN_EXPIRED, 그 외 모든 검증 실패는 AUTH_INVALID_TOKEN.
	 */
	public Long getUserIdFromAccessToken(String token) {
		return parseAccessToken(token).userId();
	}

	/**
	 * access 토큰을 검증하고 userId와 role을 꺼낸다. 오류는 getUserIdFromAccessToken과 같다.
	 * role 클레임이 없는 토큰(이 기능 전에 발급된 토큰)은 USER로 본다.
	 */
	public AccessTokenPayload parseAccessToken(String token) {
		Claims claims = parse(token, ACCESS_TOKEN_TYPE, AuthErrorCode.AUTH_INVALID_TOKEN, AuthErrorCode.AUTH_TOKEN_EXPIRED);
		Long userId = toUserId(claims, AuthErrorCode.AUTH_INVALID_TOKEN);
		String role = claims.get(ROLE_CLAIM, String.class);
		try {
			return new AccessTokenPayload(userId, role == null ? Role.USER : Role.valueOf(role));
		} catch (IllegalArgumentException e) {
			throw new BusinessException(AuthErrorCode.AUTH_INVALID_TOKEN);
		}
	}

	/**
	 * refresh 토큰을 검증하고 userId를 꺼낸다. (서버 저장소에 남아있는지는 AuthService에서 따로 확인)
	 * 만료면 AUTH_REFRESH_TOKEN_EXPIRED, 그 외 모든 검증 실패는 AUTH_INVALID_REFRESH_TOKEN.
	 */
	public Long getUserIdFromRefreshToken(String token) {
		Claims claims = parse(token, REFRESH_TOKEN_TYPE,
				AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN, AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
		return toUserId(claims, AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
	}

	private JwtBuilder tokenBuilder(Long userId, String type, Duration expiration) {
		Instant now = Instant.now();
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(userId))
				.claim(TOKEN_TYPE_CLAIM, type)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expiration)))
				.signWith(secretKey);
	}

	private Claims parse(String token, String expectedType, BaseErrorCode invalidError, BaseErrorCode expiredError) {
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
		return claims;
	}

	private Long toUserId(Claims claims, BaseErrorCode invalidError) {
		try {
			return Long.valueOf(claims.getSubject());
		} catch (NumberFormatException e) {
			throw new BusinessException(invalidError);
		}
	}

}
