package com.Wolgyesangdan.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.auth.client.KakaoOAuthClient;
import com.Wolgyesangdan.backend.domain.auth.client.KakaoUser;
import com.Wolgyesangdan.backend.domain.auth.config.AdminAccountProperties;
import com.Wolgyesangdan.backend.domain.auth.dto.AdminLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.KakaoLoginRequest;
import com.Wolgyesangdan.backend.domain.auth.dto.LoginResponse;
import com.Wolgyesangdan.backend.domain.auth.dto.TokenRefreshRequest;
import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.auth.repository.RefreshTokenRepository;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.security.AccessTokenPayload;
import com.Wolgyesangdan.backend.global.security.JwtProperties;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

	private final JwtProperties jwtProperties = new JwtProperties(
			"test-secret-key-that-is-long-enough-for-hs256", Duration.ofHours(1), Duration.ofDays(14));
	private final JwtProvider jwtProvider = new JwtProvider(jwtProperties);

	private KakaoOAuthClient kakaoOAuthClient;
	private UserRepository userRepository;
	private RefreshTokenRepository refreshTokenRepository;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		kakaoOAuthClient = Mockito.mock(KakaoOAuthClient.class);
		userRepository = Mockito.mock(UserRepository.class);
		refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
		authService = new AuthService(kakaoOAuthClient, userRepository, refreshTokenRepository, jwtProvider,
				jwtProperties, new AdminAccountProperties("admin", "1234"));
		given(userRepository.save(any(User.class))).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
	}

	@Test
	void 처음_로그인하면_회원을_만들고_isNewUser는_true() {
		given(kakaoOAuthClient.getUser("code")).willReturn(new KakaoUser("4012345678", "용민", "a@b.com"));
		given(userRepository.findByKakaoId("4012345678")).willReturn(Optional.empty());

		LoginResponse response = authService.kakaoLogin(new KakaoLoginRequest("code"));

		assertThat(response.isNewUser()).isTrue();
		assertThat(response.user()).isEqualTo(new LoginResponse.UserInfo(1L, "용민", "a@b.com"));
		assertThat(jwtProvider.getUserIdFromAccessToken(response.accessToken())).isEqualTo(1L);
		assertThat(jwtProvider.getUserIdFromRefreshToken(response.refreshToken())).isEqualTo(1L);
	}

	@Test
	void 일반_회원의_access_토큰은_USER_관리자는_ADMIN_권한이_담긴다() {
		User admin = withId(User.builder().kakaoId("1").nickname("운영자").role(Role.ADMIN).build(), 9L);
		given(kakaoOAuthClient.getUser("admin-code")).willReturn(new KakaoUser("1", "운영자", null));
		given(userRepository.findByKakaoId("1")).willReturn(Optional.of(admin));
		given(kakaoOAuthClient.getUser("code")).willReturn(new KakaoUser("4012345678", "용민", null));
		given(userRepository.findByKakaoId("4012345678")).willReturn(Optional.empty());

		String adminToken = authService.kakaoLogin(new KakaoLoginRequest("admin-code")).accessToken();
		String userToken = authService.kakaoLogin(new KakaoLoginRequest("code")).accessToken();

		assertThat(jwtProvider.parseAccessToken(adminToken)).isEqualTo(new AccessTokenPayload(9L, Role.ADMIN));
		assertThat(jwtProvider.parseAccessToken(userToken)).isEqualTo(new AccessTokenPayload(1L, Role.USER));
	}

	@Test
	void 이미_가입한_회원이면_새로_만들지_않고_isNewUser는_false() {
		User existing = withId(User.builder().kakaoId("4012345678").nickname("기존").build(), 5L);
		given(kakaoOAuthClient.getUser("code")).willReturn(new KakaoUser("4012345678", "바뀐닉네임", null));
		given(userRepository.findByKakaoId("4012345678")).willReturn(Optional.of(existing));

		LoginResponse response = authService.kakaoLogin(new KakaoLoginRequest("code"));

		assertThat(response.isNewUser()).isFalse();
		assertThat(response.user().id()).isEqualTo(5L);
		verify(userRepository, never()).save(any());
	}

	@Test
	void 닉네임_동의를_안_했으면_카카오사용자_뒤4자리로_채운다() {
		given(kakaoOAuthClient.getUser("code")).willReturn(new KakaoUser("4012345678", null, null));
		given(userRepository.findByKakaoId(anyString())).willReturn(Optional.empty());

		assertThat(authService.kakaoLogin(new KakaoLoginRequest("code")).user().nickname()).isEqualTo("카카오사용자5678");
	}

	@Test
	void 저장소에_없는_refresh_토큰이면_AUTH_INVALID_REFRESH_TOKEN() {
		given(refreshTokenRepository.deleteByTokenHash(anyString())).willReturn(0);

		assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(jwtProvider.createRefreshToken(1L))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
	}

	@Test
	void access_토큰으로_재발급하면_AUTH_INVALID_REFRESH_TOKEN() {
		assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(jwtProvider.createAccessToken(1L))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
	}

	@Test
	void 관리자_아이디_비밀번호가_맞으면_처음엔_관리자_회원을_만들고_ADMIN_토큰을_준다() {
		given(userRepository.findByKakaoId("admin:admin")).willReturn(Optional.empty());

		LoginResponse response = authService.adminLogin(new AdminLoginRequest("admin", "1234"));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
		assertThat(saved.getValue().getNickname()).isEqualTo("운영자");
		assertThat(response.isNewUser()).isTrue();
		assertThat(jwtProvider.parseAccessToken(response.accessToken())).isEqualTo(new AccessTokenPayload(1L, Role.ADMIN));
	}

	@Test
	void 관리자_회원이_이미_있으면_그_회원으로_로그인한다() {
		User admin = withId(User.builder().kakaoId("admin:admin").nickname("운영자").role(Role.ADMIN).build(), 3L);
		given(userRepository.findByKakaoId("admin:admin")).willReturn(Optional.of(admin));

		LoginResponse response = authService.adminLogin(new AdminLoginRequest("admin", "1234"));

		verify(userRepository, never()).save(any(User.class));
		assertThat(response.isNewUser()).isFalse();
		assertThat(jwtProvider.parseAccessToken(response.accessToken())).isEqualTo(new AccessTokenPayload(3L, Role.ADMIN));
	}

	@Test
	void 관리자_아이디나_비밀번호가_틀리면_AUTH_INVALID_ADMIN_CREDENTIALS() {
		assertThatThrownBy(() -> authService.adminLogin(new AdminLoginRequest("admin", "12345")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_INVALID_ADMIN_CREDENTIALS);
		assertThatThrownBy(() -> authService.adminLogin(new AdminLoginRequest("root", "1234")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_INVALID_ADMIN_CREDENTIALS);
		verify(userRepository, never()).save(any(User.class));
	}

	private static User withId(User user, Long id) {
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

}
