package com.Wolgyesangdan.backend.domain.verification.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VerificationErrorCode implements BaseErrorCode {

	VERIFICATION_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 심사 중인 인증 신청이 있습니다."),
	VERIFICATION_ALREADY_APPROVED(HttpStatus.CONFLICT, "이미 승인된 인증이 있습니다.");

	private final HttpStatus status;
	private final String message;

}
