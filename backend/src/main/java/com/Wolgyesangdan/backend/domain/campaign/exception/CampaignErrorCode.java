package com.Wolgyesangdan.backend.domain.campaign.exception;

import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CampaignErrorCode implements BaseErrorCode {

	CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 캠페인입니다."),
	CAMPAIGN_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "기간의 시작일은 종료일보다 늦을 수 없습니다."),
	CAMPAIGN_ALREADY_RUNNING(HttpStatus.CONFLICT, "예정이거나 진행 중인 캠페인이 이미 있습니다.");

	private final HttpStatus status;
	private final String message;

}
