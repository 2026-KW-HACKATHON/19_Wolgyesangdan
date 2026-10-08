package com.Wolgyesangdan.backend.domain.campaign.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.campaign.dto.ActiveCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;
import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {

	private final CampaignRepository campaignRepository;
	private final ReservationRepository reservationRepository;

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
						summarizeTrades(campaign)))
				.orElse(null);
	}

	/**
	 * 그 캠페인의 거래 완료 수와 탄소 절감량 합계. 캠페인에 연결된 물품(거점 거래)과,
	 * 캠페인 기간 안에 완료된 직거래를 함께 센다 (#248). 탄소 리포트의 "이번 캠페인"과 같은 기준이다.
	 */
	public CompletedItemSummary summarizeTrades(Campaign campaign) {
		return summarizeTrades(campaign, campaignRepository.findAll());
	}

	/** 캠페인 여러 개를 집계할 때 — 이미 읽어 둔 전체 캠페인 목록을 넘겨서 다시 조회하지 않는다 */
	public CompletedItemSummary summarizeTrades(Campaign campaign, List<Campaign> allCampaigns) {
		return reservationRepository.summarizeCompletedInCampaign(campaign.getId(), campaign.periodStartAt(),
				directTradePeriodEnd(campaign, allCampaigns));
	}

	/** 직거래를 그 캠페인의 거래로 세는 기간의 끝 (이 시각 "전"까지). 시작은 Campaign.periodStartAt */
	public LocalDateTime directTradePeriodEnd(Campaign campaign) {
		return directTradePeriodEnd(campaign, campaignRepository.findAll());
	}

	/**
	 * 한 직거래가 두 캠페인에 잡히지 않도록, 아래 중 가장 이른 시각에서 끊는다 (#250).
	 * - 캠페인 기간이 끝난 직후 / 운영 중을 끈 시각 (Campaign.closedAt)
	 * - 다음 캠페인이 시작하는 시각 — 끈 시각 기록이 없는 예전 캠페인도 다음 캠페인과 겹치지 않게 된다
	 */
	LocalDateTime directTradePeriodEnd(Campaign campaign, List<Campaign> allCampaigns) {
		LocalDateTime closedAt = campaign.closedAt();
		return allCampaigns.stream()
				.filter(other -> startsAfter(other, campaign))
				.map(Campaign::periodStartAt)
				.filter(nextStart -> nextStart.isBefore(closedAt))
				.min(Comparator.naturalOrder())
				.orElse(closedAt);
	}

	// 시작일이 같으면 나중에 만든(id가 큰) 캠페인을 다음 캠페인으로 본다
	private static boolean startsAfter(Campaign other, Campaign campaign) {
		if (other.getId().equals(campaign.getId())) {
			return false;
		}
		int byStart = other.periodStartAt().compareTo(campaign.periodStartAt());
		return byStart > 0 || (byStart == 0 && other.getId() > campaign.getId());
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
