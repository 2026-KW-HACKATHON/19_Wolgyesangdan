package com.Wolgyesangdan.backend.domain.campaign.controller;

import java.net.URI;

import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignCreateRequest;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignListResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignUpdateRequest;
import com.Wolgyesangdan.backend.domain.campaign.service.AdminCampaignService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 캠페인 관리 (ADMIN만 — SecurityConfig의 /admin/**).
 */
@RestController
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
public class AdminCampaignController {

	private final AdminCampaignService adminCampaignService;

	/** 현재(예정/진행 중) 캠페인과 지난 캠페인 목록. 현재 캠페인이 없으면 current가 null */
	@GetMapping
	public AdminCampaignListResponse getCampaigns() {
		return adminCampaignService.getCampaigns();
	}

	/** 새 캠페인. 예정/진행 중 캠페인이 이미 있으면 409 CAMPAIGN_ALREADY_RUNNING */
	@PostMapping
	public ResponseEntity<AdminCampaignResponse> createCampaign(@Valid @RequestBody AdminCampaignCreateRequest request) {
		AdminCampaignResponse created = adminCampaignService.createCampaign(request);
		return ResponseEntity.created(URI.create("/admin/campaigns/" + created.id())).body(created);
	}

	/** 캠페인 수정 — 바꿀 필드만 + status(운영 중 토글: ACTIVE / ENDED) */
	@PatchMapping("/{campaignId}")
	public AdminCampaignResponse updateCampaign(@PathVariable Long campaignId,
			@Valid @RequestBody AdminCampaignUpdateRequest request) {
		return adminCampaignService.updateCampaign(campaignId, request);
	}

}
