package com.Wolgyesangdan.backend.domain.campaign.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 새 캠페인. 소개 문구와 거점 운영 시간만 선택이다.
 * 각 기간의 시작일 ≤ 종료일은 수정과 같은 규칙이라 서비스에서 확인한다 (CAMPAIGN_PERIOD_INVALID).
 */
public record AdminCampaignCreateRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		@NotNull LocalDate registrationStartDate,
		@NotNull LocalDate registrationEndDate,
		@NotNull LocalDate applicationStartDate,
		@NotNull LocalDate applicationEndDate,
		@NotNull LocalDate pickupStartDate,
		@NotNull LocalDate pickupEndDate,
		@NotBlank @Size(max = 100) String locationName,
		@NotBlank @Size(max = 255) String locationAddress,
		@Size(max = 100) String hubHours) {

}
