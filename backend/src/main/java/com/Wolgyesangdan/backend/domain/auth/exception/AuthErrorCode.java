package com.Wolgyesangdan.backend.domain.auth.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

	AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
	AUTH_INVALID_KAKAO_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 카카오 인가 코드입니다."),
	AUTH_KAKAO_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "카카오 로그인 서버와 통신에 실패했습니다."),
	AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."),
	AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "refresh token이 만료되었습니다. 다시 로그인해주세요."),
	// 토큰은 유효한데 회원이 없음 — 회원 탈퇴 기능이 없어서 운영진이 DB에서 직접 지운 경우뿐. 다시 로그인하게 401 (#171)
	AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다. 다시 로그인해주세요."),
	AUTH_INVALID_ADMIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
	// 로그인은 했지만 권한이 없음 — 일반 회원이 관리자 API(/admin/**)를 호출한 경우
	AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

	private final HttpStatus status;
	private final String message;

}
