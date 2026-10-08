package com.Wolgyesangdan.backend.domain.item.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemErrorCode implements BaseErrorCode {

	ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 물품입니다."),
	ITEM_HIDDEN(HttpStatus.NOT_FOUND, "볼 수 없는 물품입니다."),
	ITEM_CONTACT_NOT_SET(HttpStatus.FORBIDDEN, "연락 수단을 먼저 등록해야 합니다."),
	ITEM_TRADE_METHOD_INVALID(HttpStatus.BAD_REQUEST, "캠페인 기간이 아니면 거점 수령을 선택할 수 없습니다."),
	ITEM_NOT_ACCEPTING_APPLICATIONS(HttpStatus.CONFLICT, "신청을 받는 중인 물품이 아닙니다."),
	ITEM_IMAGE_UPLOAD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "이미지 업로드 기능을 사용할 수 없습니다.");

	private final HttpStatus status;
	private final String message;

}
