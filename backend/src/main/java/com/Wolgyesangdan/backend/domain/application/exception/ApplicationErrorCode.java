package com.Wolgyesangdan.backend.domain.application.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApplicationErrorCode implements BaseErrorCode {

	APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신청입니다."),
	APPLICATION_NOT_OWNER(HttpStatus.FORBIDDEN, "본인 신청이 아닙니다."),
	APPLICATION_NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "동네 인증 승인이 필요합니다."),
	APPLICATION_OWN_ITEM(HttpStatus.FORBIDDEN, "본인이 등록한 물품에는 신청할 수 없습니다."),
	APPLICATION_CONTACT_NOT_SET(HttpStatus.FORBIDDEN, "연락 수단을 먼저 등록해야 합니다."),
	APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신청한 물품입니다."),
	APPLICATION_ITEM_NOT_OPEN(HttpStatus.CONFLICT, "신청 가능한 상태의 물품이 아닙니다."),
	APPLICATION_ALREADY_SELECTED(HttpStatus.CONFLICT, "이미 배정이 확정된 신청은 취소할 수 없습니다."),
	APPLICATION_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소한 신청입니다.");

	private final HttpStatus status;
	private final String message;

}
