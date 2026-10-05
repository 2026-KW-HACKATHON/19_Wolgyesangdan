package com.Wolgyesangdan.backend.domain.verification.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VerificationErrorCode implements BaseErrorCode {

	VERIFICATION_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 심사 중인 인증 신청이 있습니다."),
	VERIFICATION_INVALID_STUDENT_ID(HttpStatus.BAD_REQUEST, "학번 형식이 올바르지 않습니다.");

	private final HttpStatus status;
	private final String message;

}
