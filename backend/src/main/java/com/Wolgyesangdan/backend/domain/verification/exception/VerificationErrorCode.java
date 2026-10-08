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
	VERIFICATION_DOCUMENT_UPLOAD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서류 업로드 기능을 사용할 수 없습니다."),

	// GPS 동네 인증 (#278)
	VERIFICATION_LOCATION_INACCURATE(HttpStatus.BAD_REQUEST, "위치 오차가 너무 큽니다."),
	VERIFICATION_OUTSIDE_NEIGHBORHOOD(HttpStatus.BAD_REQUEST, "월계1동 안에서만 동네 인증을 할 수 있습니다."),
	VERIFICATION_LOCATION_LOOKUP_FAILED(HttpStatus.BAD_GATEWAY, "현재 위치의 동네를 확인할 수 없습니다."),

	// 관리자 심사
	VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 신청을 찾을 수 없습니다."),
	VERIFICATION_ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 처리된 인증 신청입니다."),
	VERIFICATION_ADMISSION_YEAR_INVALID(HttpStatus.BAD_REQUEST, "입학 연도는 올해 또는 내년이어야 합니다."),
	// 파일 없이 신청된 예전 건이거나, 검토 후 보관 기간이 지나 지운 경우
	VERIFICATION_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "서류 파일이 없습니다."),
	VERIFICATION_DOCUMENT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서류를 불러올 수 없습니다.");

	private final HttpStatus status;
	private final String message;

}
