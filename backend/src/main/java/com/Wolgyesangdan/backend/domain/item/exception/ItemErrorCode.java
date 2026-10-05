package com.Wolgyesangdan.backend.domain.item.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ItemErrorCode implements BaseErrorCode {

	ITEM_IMAGE_UPLOAD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "지금은 사진을 올릴 수 없습니다. 잠시 후 다시 시도해주세요.");

	private final HttpStatus status;
	private final String message;

}
