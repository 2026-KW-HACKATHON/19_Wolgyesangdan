package com.Wolgyesangdan.backend.domain.item.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemErrorCode implements BaseErrorCode {

	ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 물품입니다.");

	private final HttpStatus status;
	private final String message;

}
