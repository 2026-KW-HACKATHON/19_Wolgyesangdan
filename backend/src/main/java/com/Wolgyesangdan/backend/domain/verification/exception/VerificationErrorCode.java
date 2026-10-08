package com.Wolgyesangdan.backend.domain.verification.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VerificationErrorCode implements BaseErrorCode {

	VERIFICATION_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 심사 중인 인증 신청이 있습니다."),
	VERIFICATION_ALREADY_APPROVED(HttpStatus.CONFLICT, "이미 승인된 인증이 있습니다."),
	// 서버에 S3 설정이 없을 때 (로컬에 AWS 키가 없는 팀원 환경). 다른 API는 정상 동작
	VERIFICATION_DOCUMENT_UPLOAD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서류 업로드 기능을 사용할 수 없습니다.");

	private final HttpStatus status;
	private final String message;

}
