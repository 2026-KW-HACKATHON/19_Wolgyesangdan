package com.Wolgyesangdan.backend.domain.campaign.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignCreateRequest;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignListResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignListResponse.PastCampaign;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignUpdateRequest;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.exception.CampaignErrorCode;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 캠페인 관리 (#209). 예정이거나 진행 중인 캠페인은 동시에 하나만 둔다.
 * 예정/진행 중/끝남은 회원 API와 같은 기준(Campaign.statusOn)으로 본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCampaignService {

	private final CampaignRepository campaignRepository;
	private final CampaignService campaignService;
	private final ItemRepository itemRepository;

	public AdminCampaignListResponse getCampaigns() {
		return getCampaigns(LocalDate.now());
	}

	AdminCampaignListResponse getCampaigns(LocalDate today) {
		AdminCampaignResponse current = campaignService.findCurrentCampaign(today)
				.map(campaign -> AdminCampaignResponse.of(campaign, today))
				.orElse(null);
		// 캠페인은 회차별로 몇 개뿐이라 끝난 캠페인마다 집계 쿼리를 한 번씩 보낸다
		List<PastCampaign> past = campaignRepository.findAll().stream()
				.filter(campaign -> campaign.statusOn(today) == CampaignStatus.ENDED)
				.sorted(Comparator.comparing(Campaign::periodEnd).thenComparing(Campaign::getId).reversed())
				.map(campaign -> PastCampaign.of(campaign,
						itemRepository.summarizeCompletedByCampaignId(campaign.getId())))
				.toList();
		return new AdminCampaignListResponse(current, past);
	}

	@Transactional
	public AdminCampaignResponse createCampaign(AdminCampaignCreateRequest request) {
		return createCampaign(request, LocalDate.now());
	}

	AdminCampaignResponse createCampaign(AdminCampaignCreateRequest request, LocalDate today) {
		if (campaignService.findCurrentCampaign(today).isPresent()) {
			throw new BusinessException(CampaignErrorCode.CAMPAIGN_ALREADY_RUNNING);
		}
		Campaign campaign = Campaign.builder()
				.name(request.name().strip())
				.description(blankToNull(request.description()))
				.registrationStartDate(request.registrationStartDate())
				.registrationEndDate(request.registrationEndDate())
				.applicationStartDate(request.applicationStartDate())
				.applicationEndDate(request.applicationEndDate())
				.pickupStartDate(request.pickupStartDate())
				.pickupEndDate(request.pickupEndDate())
				.locationName(request.locationName().strip())
				.locationAddress(request.locationAddress().strip())
				.hubHours(blankToNull(request.hubHours()))
				.build();
		validatePeriods(campaign);
		campaign.resume(today);
		return AdminCampaignResponse.of(campaignRepository.save(campaign), today);
	}

	@Transactional
	public AdminCampaignResponse updateCampaign(Long campaignId, AdminCampaignUpdateRequest request) {
		return updateCampaign(campaignId, request, LocalDate.now());
	}

	/**
	 * 보낸 필드만 바꾼다. status는 운영 중 토글 — ENDED면 끄고, ACTIVE면 다시 켠다.
	 * 끝난 캠페인이 이 수정으로 다시 예정/진행 중이 되는데 다른 예정/진행 중 캠페인이 있으면 409.
	 * 거점 이름은 만들 때 필수이고 비울 수 없어서, 켤 때 따로 확인하지 않아도 항상 있다.
	 */
	AdminCampaignResponse updateCampaign(Long campaignId, AdminCampaignUpdateRequest request, LocalDate today) {
		Campaign campaign = campaignRepository.findById(campaignId)
				.orElseThrow(() -> new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
		boolean wasRunning = campaign.statusOn(today) != CampaignStatus.ENDED;

		campaign.update(
				request.name() == null ? campaign.getName() : request.name().strip(),
				request.description() == null ? campaign.getDescription() : blankToNull(request.description()),
				orElse(request.registrationStartDate(), campaign.getRegistrationStartDate()),
				orElse(request.registrationEndDate(), campaign.getRegistrationEndDate()),
				orElse(request.applicationStartDate(), campaign.getApplicationStartDate()),
				orElse(request.applicationEndDate(), campaign.getApplicationEndDate()),
				orElse(request.pickupStartDate(), campaign.getPickupStartDate()),
				orElse(request.pickupEndDate(), campaign.getPickupEndDate()),
				request.locationName() == null ? campaign.getLocationName() : request.locationName().strip(),
				request.locationAddress() == null ? campaign.getLocationAddress() : request.locationAddress().strip(),
				request.hubHours() == null ? campaign.getHubHours() : blankToNull(request.hubHours()));
		validatePeriods(campaign);
		if (request.status() == CampaignStatus.ENDED) {
			campaign.end();
		} else if (request.status() == CampaignStatus.ACTIVE) {
			campaign.resume(today);
		}

		boolean running = campaign.statusOn(today) != CampaignStatus.ENDED;
		if (!wasRunning && running && existsOtherRunning(campaign, today)) {
			throw new BusinessException(CampaignErrorCode.CAMPAIGN_ALREADY_RUNNING);
		}
		return AdminCampaignResponse.of(campaign, today);
	}

	private boolean existsOtherRunning(Campaign campaign, LocalDate today) {
		return campaignRepository.findAll().stream()
				.anyMatch(other -> !other.getId().equals(campaign.getId())
						&& other.statusOn(today) != CampaignStatus.ENDED);
	}

	// 각 기간의 시작일 ≤ 종료일 (같은 날은 허용)
	private static void validatePeriods(Campaign campaign) {
		validatePeriod("물품 등록", campaign.getRegistrationStartDate(), campaign.getRegistrationEndDate());
		validatePeriod("신청", campaign.getApplicationStartDate(), campaign.getApplicationEndDate());
		validatePeriod("수령", campaign.getPickupStartDate(), campaign.getPickupEndDate());
	}

	private static void validatePeriod(String label, LocalDate start, LocalDate end) {
		if (start.isAfter(end)) {
			throw new BusinessException(CampaignErrorCode.CAMPAIGN_PERIOD_INVALID,
					label + " 기간의 시작일은 종료일보다 늦을 수 없습니다.");
		}
	}

	private static LocalDate orElse(LocalDate value, LocalDate fallback) {
		return value == null ? fallback : value;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
