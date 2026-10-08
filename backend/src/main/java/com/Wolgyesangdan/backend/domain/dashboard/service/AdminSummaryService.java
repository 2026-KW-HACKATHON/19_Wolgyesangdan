package com.Wolgyesangdan.backend.domain.dashboard.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;
import com.Wolgyesangdan.backend.domain.dashboard.dto.AdminSummaryResponse;
import com.Wolgyesangdan.backend.domain.dashboard.dto.AdminSummaryResponse.CurrentCampaign;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;
import com.Wolgyesangdan.backend.domain.inquiry.repository.InquiryRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드 요약 (#216). 자기 엔티티 없이 각 도메인의 건수를 모은다.
 * 캐시 없이 조회할 때마다 센다 — 건수 쿼리 3번 + 캠페인 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSummaryService {

	/** 관리자 서류 화면에 나오는 유형 — 심사 없이 승인되는 동네 인증은 뺀다 */
	private static final List<VerificationType> PRIORITY_TYPES = Arrays.stream(VerificationType.values())
			.filter(VerificationType::isPriority)
			.toList();

	private final PriorityVerificationRepository priorityVerificationRepository;
	private final InquiryRepository inquiryRepository;
	private final ItemRepository itemRepository;
	private final CampaignService campaignService;

	public AdminSummaryResponse getSummary() {
		return getSummary(LocalDate.now());
	}

	AdminSummaryResponse getSummary(LocalDate today) {
		CurrentCampaign currentCampaign = campaignService.findCurrentCampaign(today)
				.map(campaign -> CurrentCampaign.of(campaign, today, campaignService.summarizeTrades(campaign).count()))
				.orElse(null);
		return new AdminSummaryResponse(
				priorityVerificationRepository.countByVerificationTypeInAndStatus(PRIORITY_TYPES, VerificationStatus.PENDING),
				inquiryRepository.countByStatus(InquiryStatus.OPEN),
				itemRepository.countByHiddenTrue(),
				currentCampaign);
	}

}
