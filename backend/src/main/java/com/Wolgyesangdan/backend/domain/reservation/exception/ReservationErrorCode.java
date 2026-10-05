package com.Wolgyesangdan.backend.domain.reservation.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements BaseErrorCode {

	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."),
	RESERVATION_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "본인과 관련된 예약이 아닙니다."),
	RESERVATION_ALREADY_RECONFIRMED(HttpStatus.CONFLICT, "이미 재확인을 완료한 예약입니다."),
	RESERVATION_NOT_RECONFIRMABLE(HttpStatus.CONFLICT, "재확인할 수 없는 예약입니다."),
	RESERVATION_RECONFIRMATION_EXPIRED(HttpStatus.CONFLICT, "재확인 응답 기한이 지났습니다.");

	private final HttpStatus status;
	private final String message;

}
