package com.Wolgyesangdan.backend.domain.campaign.dto;

import java.time.LocalDate;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;

/**
 * 관리자 캠페인 화면.
 *
 * @param current 예정이거나 진행 중인 캠페인, 없으면 null
 * @param past 끝난 캠페인 — 최근에 끝난 순
 */
public record AdminCampaignListResponse(AdminCampaignResponse current, List<PastCampaign> past) {

	/**
	 * @param startDate 캠페인 전체 기간의 시작일 (등록/신청/수령 기간 중 가장 이른 시작일)
	 * @param endDate 캠페인 전체 기간의 종료일 (등록/신청/수령 기간 중 가장 늦은 종료일)
	 */
	public record PastCampaign(
			Long id,
			String name,
			LocalDate startDate,
			LocalDate endDate,
			int reusedCount,
			int carbonReductionKg) {

		public static PastCampaign of(Campaign campaign, CompletedItemSummary completed) {
			return new PastCampaign(
					campaign.getId(),
					campaign.getName(),
					campaign.periodStart(),
					campaign.periodEnd(),
					Math.toIntExact(completed.count()),
					Math.toIntExact(completed.carbonReductionKg()));
		}

	}

}
