package com.Wolgyesangdan.backend.domain.campaign.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CampaignTest {

	// 등록 9/15~10/4, 신청 9/20~10/4, 수령 9/20~10/6 → 전체 기간 9/15 ~ 10/6
	private final Campaign campaign = Campaign.builder()
			.registrationStartDate(LocalDate.of(2026, 9, 15))
			.registrationEndDate(LocalDate.of(2026, 10, 4))
			.applicationStartDate(LocalDate.of(2026, 9, 20))
			.applicationEndDate(LocalDate.of(2026, 10, 4))
			.pickupStartDate(LocalDate.of(2026, 9, 20))
			.pickupEndDate(LocalDate.of(2026, 10, 6))
			.build();

	@Test
	void 전체_기간은_가장_이른_시작일부터_가장_늦은_종료일까지() {
		assertThat(campaign.periodStart()).isEqualTo(LocalDate.of(2026, 9, 15));
		assertThat(campaign.periodEnd()).isEqualTo(LocalDate.of(2026, 10, 6));
	}

	@Test
	void 날짜로_진행_상태를_판단한다() {
		assertThat(campaign.statusOn(LocalDate.of(2026, 9, 14))).isEqualTo(CampaignStatus.PLANNED);
		assertThat(campaign.statusOn(LocalDate.of(2026, 9, 15))).isEqualTo(CampaignStatus.ACTIVE); // 시작일 당일
		assertThat(campaign.statusOn(LocalDate.of(2026, 10, 1))).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(campaign.statusOn(LocalDate.of(2026, 10, 6))).isEqualTo(CampaignStatus.ACTIVE); // 종료일 당일
		assertThat(campaign.statusOn(LocalDate.of(2026, 10, 7))).isEqualTo(CampaignStatus.ENDED);
	}

	@Test
	void 운영_중을_끄면_기간이_남아_있어도_끝난_캠페인이다() {
		campaign.end();

		assertThat(campaign.statusOn(LocalDate.of(2026, 9, 14))).isEqualTo(CampaignStatus.ENDED);
		assertThat(campaign.statusOn(LocalDate.of(2026, 10, 1))).isEqualTo(CampaignStatus.ENDED);
	}

	@Test
	void 운영_중을_다시_켜면_날짜로_판단한다() {
		campaign.end();

		campaign.resume(LocalDate.of(2026, 9, 14));

		assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PLANNED);
		assertThat(campaign.statusOn(LocalDate.of(2026, 9, 14))).isEqualTo(CampaignStatus.PLANNED);
		assertThat(campaign.statusOn(LocalDate.of(2026, 10, 1))).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(campaign.statusOn(LocalDate.of(2026, 10, 7))).isEqualTo(CampaignStatus.ENDED);

		campaign.end();
		campaign.resume(LocalDate.of(2026, 10, 1));

		assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
	}

}
