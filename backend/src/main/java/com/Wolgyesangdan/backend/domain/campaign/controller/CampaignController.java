package com.Wolgyesangdan.backend.domain.campaign.controller;

import com.Wolgyesangdan.backend.domain.campaign.dto.ActiveCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.NullNode;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {

	private final CampaignService campaignService;

	/**
	 * 진행 중/예정 캠페인이 없으면 200 + JSON null.
	 * 컨트롤러가 그냥 null을 반환하면 본문이 비어서(Content-Length: 0) 프론트의 response.json()이 실패하므로,
	 * 명세대로 null을 명시적으로 내려준다.
	 */
	@GetMapping("/active")
	public ResponseEntity<?> getActiveCampaign() {
		ActiveCampaignResponse response = campaignService.getActiveCampaign();
		if (response == null) {
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NullNode.getInstance());
		}
		return ResponseEntity.ok(response);
	}

}
