package com.Wolgyesangdan.backend.global.exception;

import java.util.List;

public record ErrorResponse(int status, String code, String message, List<FieldError> errors) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(BaseErrorCode errorCode) {
		return of(errorCode, errorCode.getMessage());
	}

	public static ErrorResponse of(BaseErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(), message, List.of());
	}

	public static ErrorResponse of(BaseErrorCode errorCode, List<FieldError> errors) {
		return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage(), errors);
	}

}
