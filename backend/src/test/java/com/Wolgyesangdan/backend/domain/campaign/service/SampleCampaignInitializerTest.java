package com.Wolgyesangdan.backend.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;

class SampleCampaignInitializerTest {

	private final CampaignRepository campaignRepository = Mockito.mock(CampaignRepository.class);
	private final SampleCampaignInitializer initializer = new SampleCampaignInitializer(campaignRepository);

	@Test
	void 캠페인이_하나도_없으면_샘플을_만든다() {
		given(campaignRepository.count()).willReturn(0L);
		given(campaignRepository.save(any(Campaign.class))).willAnswer(invocation -> invocation.getArgument(0));

		initializer.run(new DefaultApplicationArguments());

		ArgumentCaptor<Campaign> saved = ArgumentCaptor.forClass(Campaign.class);
		verify(campaignRepository).save(saved.capture());
		assertThat(saved.getValue().getName()).isEqualTo(SampleCampaignInitializer.SAMPLE_NAME);
	}

	@Test
	void 캠페인이_이미_있으면_아무것도_안_한다() {
		given(campaignRepository.count()).willReturn(1L);

		initializer.run(new DefaultApplicationArguments());

		verify(campaignRepository, never()).save(any());
	}

	@Test
	void 샘플은_기준일에_등록_신청_수령_기간이_모두_진행_중이다() {
		LocalDate today = LocalDate.of(2026, 10, 4);

		Campaign campaign = SampleCampaignInitializer.createSample(today);

		assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(today).isBetween(campaign.getRegistrationStartDate(), campaign.getRegistrationEndDate());
		assertThat(today).isBetween(campaign.getApplicationStartDate(), campaign.getApplicationEndDate());
		assertThat(today).isBetween(campaign.getPickupStartDate(), campaign.getPickupEndDate());
		// 신청이 끝난 뒤에도 수령할 수 있어야 함
		assertThat(campaign.getPickupEndDate()).isAfterOrEqualTo(campaign.getApplicationEndDate());
	}

	@Test
	void local_프로필에서만_등록된다() {
		assertThat(SampleCampaignInitializer.class.getAnnotation(Profile.class).value()).containsExactly("local");
	}

}
