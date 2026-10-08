package com.Wolgyesangdan.backend.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.auth.client.KakaoOAuthClient;
import com.Wolgyesangdan.backend.domain.auth.client.KakaoUser;
import com.Wolgyesangdan.backend.domain.auth.config.AdminAccountProperties;
import com.Wolgyesangdan.backend.domain.auth.dto.AdminLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.DevLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.KakaoLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.LoginResponse;
import com.Wolgyesangdan.backend.domain.auth.dto.TokenRefreshRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.TokenResponse;
import com.Wolgyesangdan.backend.domain.auth.entity.RefreshToken;
import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.auth.repository.RefreshTokenRepository;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
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
	// 관리자 계정은 카카오 회원이 아니라서 kakao_id 자리에 이 접두사를 붙여 구분한다 (카카오 회원번호는 숫자뿐이라 겹치지 않음)
	private static final String ADMIN_ACCOUNT_KEY_PREFIX = "admin:";
	private static final String ADMIN_NICKNAME = "운영자";

	private final KakaoOAuthClient kakaoOAuthClient;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProvider jwtProvider;
	private final JwtProperties jwtProperties;
	private final AdminAccountProperties adminAccountProperties;

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
	 * 관리자 웹 로그인. 설정의 관리자 아이디·비밀번호와 맞으면 관리자(ADMIN) 회원으로 토큰을 발급한다.
	 * 관리자 회원은 처음 로그인할 때 만든다. 틀리면 아이디·비밀번호 중 무엇이 틀렸는지 구분하지 않고 401.
	 */
	@Transactional
	public LoginResponse adminLogin(AdminLoginRequest request) {
		boolean idMatches = constantTimeEquals(request.loginId(), adminAccountProperties.loginId());
		boolean passwordMatches = constantTimeEquals(request.password(), adminAccountProperties.password());
		if (!idMatches || !passwordMatches) {
			throw new BusinessException(AuthErrorCode.AUTH_INVALID_ADMIN_CREDENTIALS);
		}

		String accountKey = ADMIN_ACCOUNT_KEY_PREFIX + adminAccountProperties.loginId();
		Optional<User> existingUser = userRepository.findByKakaoId(accountKey);
		User user = existingUser.orElseGet(() -> userRepository.save(User.builder()
				.kakaoId(accountKey)
				.nickname(ADMIN_NICKNAME)
				.role(Role.ADMIN)
				.build()));
		return LoginResponse.of(issueTokens(user), existingUser.isEmpty(), user);
	}

	// 문자열 비교에 걸린 시간으로 맞은 글자 수를 추측하지 못하게 한다
	private static boolean constantTimeEquals(String input, String expected) {
		return expected != null && MessageDigest.isEqual(
				input.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
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
				.role(Boolean.TRUE.equals(request.admin()) ? Role.ADMIN : Role.USER)
				.build()));
		return LoginResponse.of(issueTokens(user), existingUser.isEmpty(), user);
	}

	private TokenResponse issueTokens(User user) {
		refreshTokenRepository.deleteExpiredByUserId(user.getId(), LocalDateTime.now());

		String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
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
