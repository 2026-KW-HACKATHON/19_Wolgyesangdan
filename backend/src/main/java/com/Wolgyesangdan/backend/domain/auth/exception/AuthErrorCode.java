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
	AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "refresh token이 만료되었습니다. 다시 로그인해주세요.");

	private final HttpStatus status;
	private final String message;

}
