package com.Wolgyesangdan.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 에러 코드 enum이 구현하는 인터페이스.
 * 도메인별로 enum을 나눠서(UserErrorCode 등) 이 인터페이스를 구현하면 된다.
 */
public interface BaseErrorCode {
	String name();

	HttpStatus getStatus();

	String getMessage();

	default String getCode() {
		return name();
	}

}
