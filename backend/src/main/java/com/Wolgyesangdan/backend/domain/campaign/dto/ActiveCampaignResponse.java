package com.Wolgyesangdan.backend.domain.campaign.dto;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;

public record ActiveCampaignResponse(
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
		String hubHours,
		int reusedCount,
		int carbonReductionKg) {

	public static ActiveCampaignResponse of(Campaign campaign, CampaignStatus status, CompletedItemSummary completed) {
		return new ActiveCampaignResponse(
				campaign.getId(),
				campaign.getName(),
				campaign.getDescription(),
				status,
				campaign.getRegistrationStartDate(),
				campaign.getRegistrationEndDate(),
				campaign.getApplicationStartDate(),
				campaign.getApplicationEndDate(),
				campaign.getPickupStartDate(),
				campaign.getPickupEndDate(),
				campaign.getLocationName(),
				campaign.getLocationAddress(),
				campaign.getHubHours(),
				Math.toIntExact(completed.count()),
				Math.toIntExact(completed.carbonReductionKg()));
	}

}
