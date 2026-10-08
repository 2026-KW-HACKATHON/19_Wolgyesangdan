package com.Wolgyesangdan.backend.domain.dashboard.dto;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;

/**
 * 관리자 대시보드 카드 · 사이드바 건수 배지 · 상단 바 캠페인 칩에 쓰는 요약.
 *
 * @param pendingVerifications 검토 대기 중인 우선배정 서류 수
 * @param openInquiries 답변 대기 중인 문의 수
 * @param hiddenItems 숨긴 물품 수
 * @param currentCampaign 진행 중(없으면 다음 예정) 캠페인, 없으면 null
 */
public record AdminSummaryResponse(
		long pendingVerifications,
		long openInquiries,
		long hiddenItems,
		CurrentCampaign currentCampaign) {

	/**
	 * @param status 오늘 기준 진행 상태 — PLANNED 또는 ACTIVE
	 * @param startDate 캠페인 전체 기간의 시작일 (등록/신청/수령 기간 중 가장 이른 시작일)
	 * @param endDate 캠페인 전체 기간의 종료일 (등록/신청/수령 기간 중 가장 늦은 종료일)
	 * @param reusedCount 이 캠페인에서 거래가 끝난 물품 수
	 */
	public record CurrentCampaign(
			Long id,
			String name,
			CampaignStatus status,
			LocalDate startDate,
			LocalDate endDate,
			long reusedCount) {

		public static CurrentCampaign of(Campaign campaign, LocalDate today, long reusedCount) {
			return new CurrentCampaign(
					campaign.getId(),
					campaign.getName(),
					campaign.statusOn(today),
					campaign.periodStart(),
					campaign.periodEnd(),
					reusedCount);
		}

	}

}
