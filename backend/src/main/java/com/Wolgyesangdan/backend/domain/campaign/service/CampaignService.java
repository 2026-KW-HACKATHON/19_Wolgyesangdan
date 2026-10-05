package com.Wolgyesangdan.backend.domain.campaign.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.campaign.dto.ActiveCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {

	private final CampaignRepository campaignRepository;
	private final ItemRepository itemRepository;

	/**
	 * 지금 진행 중인 캠페인, 없으면 가장 먼저 시작할 예정 캠페인. 둘 다 없으면 null.
	 * 진행 중/예정은 DB status 컬럼이 아니라 날짜로 판단한다 (Campaign.statusOn).
	 */
	public ActiveCampaignResponse getActiveCampaign() {
		return getActiveCampaign(LocalDate.now());
	}

	ActiveCampaignResponse getActiveCampaign(LocalDate today) {
		return findCurrentCampaign(today)
				.map(campaign -> ActiveCampaignResponse.of(campaign, campaign.statusOn(today),
						itemRepository.summarizeCompletedByCampaignId(campaign.getId())))
				.orElse(null);
	}

	/**
	 * today 기준 진행 중인 캠페인, 없으면 가장 먼저 시작할 예정 캠페인.
	 * 탄소절감 리포트의 "이번 캠페인" 범위도 이 캠페인을 쓴다.
	 */
	public Optional<Campaign> findCurrentCampaign(LocalDate today) {
		// 캠페인은 운영진이 회차별로 몇 개만 넣으므로 전부 읽어서 고른다
		List<Campaign> campaigns = campaignRepository.findAll();
		return findEarliestStarting(campaigns, today, CampaignStatus.ACTIVE)
				.or(() -> findEarliestStarting(campaigns, today, CampaignStatus.PLANNED));
	}

	private Optional<Campaign> findEarliestStarting(List<Campaign> campaigns, LocalDate today, CampaignStatus status) {
		return campaigns.stream()
				.filter(campaign -> campaign.statusOn(today) == status)
				.min(Comparator.comparing(Campaign::periodStart));
	}

}
