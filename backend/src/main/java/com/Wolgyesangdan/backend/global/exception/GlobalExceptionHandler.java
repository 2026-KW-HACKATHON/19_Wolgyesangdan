package com.Wolgyesangdan.backend.global.exception;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		BaseErrorCode errorCode = e.getErrorCode();
		log.warn("BusinessException: {} - {}", errorCode, e.getMessage());
		return ResponseEntity.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unhandled exception", e);
		BaseErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.status(status)
				.body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, errors));
	}

	// 스프링 MVC 기본 예외(405, 404, 400 등)도 같은 ErrorResponse 형식으로 내려준다.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
		String code = httpStatus != null ? httpStatus.name() : "ERROR";
		String message = body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null
				? problemDetail.getDetail()
				: httpStatus != null ? httpStatus.getReasonPhrase() : "Error";
		return ResponseEntity.status(statusCode)
				.headers(headers)
				.body(new ErrorResponse(statusCode.value(), code, message, List.of()));
	}

}
