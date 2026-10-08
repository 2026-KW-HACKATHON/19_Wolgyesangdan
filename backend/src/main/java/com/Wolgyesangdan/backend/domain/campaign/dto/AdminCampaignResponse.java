package com.Wolgyesangdan.backend.domain.campaign.dto;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;

/**
 * 관리자 캠페인 폼에 채우는 캠페인 전체 정보.
 *
 * @param status 오늘 기준 진행 상태(Campaign.statusOn). ENDED가 아니면 "운영 중"이 켜진 캠페인이다
 */
public record AdminCampaignResponse(
		Long id,
		String name,
		String description,
		CampaignStatus status,
		LocalDate registrationStartDate,
		LocalDate registrationEndDate,
		LocalDate applicationStartDate,
		LocalDate applicationEndDate,
		LocalDate pickupStartDate,
		LocalDate pickupEndDate,
		String locationName,
		String locationAddress,
		String hubHours) {

	public static AdminCampaignResponse of(Campaign campaign, LocalDate today) {
		return new AdminCampaignResponse(
				campaign.getId(),
				campaign.getName(),
				campaign.getDescription(),
				campaign.statusOn(today),
				campaign.getRegistrationStartDate(),
				campaign.getRegistrationEndDate(),
				campaign.getApplicationStartDate(),
				campaign.getApplicationEndDate(),
				campaign.getPickupStartDate(),
				campaign.getPickupEndDate(),
				campaign.getLocationName(),
				campaign.getLocationAddress(),
				campaign.getHubHours());
	}

}
