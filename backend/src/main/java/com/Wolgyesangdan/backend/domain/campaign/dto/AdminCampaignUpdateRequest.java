package com.Wolgyesangdan.backend.domain.campaign.dto;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 캠페인 수정. 바꿀 필드만 보내고, 안 보낸(null) 필드는 그대로 둔다.
 * 소개 문구·거점 운영 시간은 빈 문자열을 보내면 지워진다. 이름·거점 이름·주소는 비울 수 없다.
 *
 * @param status 운영 중 토글 — ACTIVE(켜기) / ENDED(끄기)
 */
public record AdminCampaignUpdateRequest(
		@Pattern(regexp = NOT_BLANK, message = "비워둘 수 없습니다.") @Size(max = 100) String name,
		String description,
		LocalDate registrationStartDate,
		LocalDate registrationEndDate,
		LocalDate applicationStartDate,
		LocalDate applicationEndDate,
		LocalDate pickupStartDate,
		LocalDate pickupEndDate,
		@Pattern(regexp = NOT_BLANK, message = "비워둘 수 없습니다.") @Size(max = 100) String locationName,
		@Pattern(regexp = NOT_BLANK, message = "비워둘 수 없습니다.") @Size(max = 255) String locationAddress,
		@Size(max = 100) String hubHours,
		CampaignStatus status) {

	// 공백이 아닌 글자가 하나라도 있어야 한다 (null은 "바꾸지 않음"이라 통과)
	private static final String NOT_BLANK = "(?s).*\\S.*";

	@AssertTrue(message = "ACTIVE 또는 ENDED만 보낼 수 있습니다.")
	public boolean isStatusToggle() {
		return status != CampaignStatus.PLANNED;
	}

}
