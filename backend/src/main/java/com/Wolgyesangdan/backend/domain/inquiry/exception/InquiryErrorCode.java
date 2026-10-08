package com.Wolgyesangdan.backend.domain.inquiry.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InquiryErrorCode implements BaseErrorCode {

	INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문의입니다."),
	INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "이미 답변한 문의입니다.");

	private final HttpStatus status;
	private final String message;

}
