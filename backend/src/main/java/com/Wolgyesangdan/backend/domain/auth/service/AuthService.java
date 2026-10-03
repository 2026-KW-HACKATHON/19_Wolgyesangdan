package com.Wolgyesangdan.backend.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.auth.client.KakaoOAuthClient;
import com.Wolgyesangdan.backend.domain.auth.client.KakaoUser;
import com.Wolgyesangdan.backend.domain.auth.dto.DevLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.KakaoLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.LoginResponse;
import com.Wolgyesangdan.backend.domain.auth.dto.TokenRefreshRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.TokenResponse;
import com.Wolgyesangdan.backend.domain.auth.entity.RefreshToken;
import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.auth.repository.RefreshTokenRepository;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.security.JwtProperties;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String DEFAULT_NICKNAME_PREFIX = "카카오사용자";
	private static final int NICKNAME_MAX_LENGTH = 50;

	private final KakaoOAuthClient kakaoOAuthClient;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProvider jwtProvider;
	private final JwtProperties jwtProperties;

	/**
	 * 카카오 인가 코드로 로그인. kakao_id로 가입된 회원이 없으면 새로 만든다.
	 */
	@Transactional
	public LoginResponse kakaoLogin(KakaoLoginRequest request) {
		KakaoUser kakaoUser = kakaoOAuthClient.getUser(request.authorizationCode());

		Optional<User> existingUser = userRepository.findByKakaoId(kakaoUser.kakaoId());
		User user = existingUser.orElseGet(() -> userRepository.save(User.builder()
				.kakaoId(kakaoUser.kakaoId())
				.nickname(resolveNickname(kakaoUser))
				.email(kakaoUser.email())
				.build()));

		return LoginResponse.of(issueTokens(user), existingUser.isEmpty(), user);
	}

	/**
	 * refresh 토큰 회전: 넘어온 토큰을 폐기하고 access/refresh 둘 다 새로 발급한다.
	 */
	@Transactional
	public TokenResponse refresh(TokenRefreshRequest request) {
		Long userId = jwtProvider.getUserIdFromRefreshToken(request.refreshToken());

		if (refreshTokenRepository.deleteByTokenHash(hash(request.refreshToken())) == 0) {
			throw new BusinessException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
		}
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN));
		return issueTokens(user);
	}

	/**
	 * 카카오 연동 없이 토큰을 받기 위한 로컬 개발용 로그인.
	 */
	@Transactional
	public LoginResponse devLogin(DevLoginRequest request) {
		Optional<User> existingUser = userRepository.findByKakaoId(request.kakaoId());
		User user = existingUser.orElseGet(() -> userRepository.save(User.builder()
				.kakaoId(request.kakaoId())
				.nickname(request.nickname())
				.build()));
		return LoginResponse.of(issueTokens(user), existingUser.isEmpty(), user);
	}

	private TokenResponse issueTokens(User user) {
		refreshTokenRepository.deleteExpiredByUserId(user.getId(), LocalDateTime.now());

		String accessToken = jwtProvider.createAccessToken(user.getId());
		String refreshToken = jwtProvider.createRefreshToken(user.getId());
		refreshTokenRepository.save(RefreshToken.builder()
				.user(user)
				.tokenHash(hash(refreshToken))
				.expiresAt(LocalDateTime.now().plus(jwtProperties.refreshTokenExpiration()))
				.build());
		return new TokenResponse(accessToken, refreshToken);
	}

	// 카카오 닉네임 제공에 동의하지 않은 경우 "카카오사용자1234"(kakao_id 끝 4자리) 형태로 채운다.
	private String resolveNickname(KakaoUser kakaoUser) {
		String nickname = kakaoUser.nickname();
		if (nickname == null || nickname.isBlank()) {
			String kakaoId = kakaoUser.kakaoId();
			return DEFAULT_NICKNAME_PREFIX + kakaoId.substring(Math.max(0, kakaoId.length() - 4));
		}
		return nickname.length() > NICKNAME_MAX_LENGTH ? nickname.substring(0, NICKNAME_MAX_LENGTH) : nickname;
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
