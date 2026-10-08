package com.Wolgyesangdan.backend.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import com.Wolgyesangdan.backend.domain.campaign.dto.ActiveCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;
import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class CampaignServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

	private final CampaignRepository campaignRepository = Mockito.mock(CampaignRepository.class);
	private final ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
	private final CampaignService campaignService = new CampaignService(campaignRepository, reservationRepository);

	@BeforeEach
	void setUp() {
		given(reservationRepository.summarizeCompletedInCampaign(any(), any(), any())).willReturn(new CompletedItemSummary(0, 0));
	}

	@Test
	void 진행_중인_캠페인을_예정_캠페인보다_먼저_내려준다() {
		given(campaignRepository.findAll()).willReturn(List.of(
				campaign(1L, TODAY.plusDays(3), TODAY.plusDays(20)),     // 예정
				campaign(2L, TODAY.minusDays(10), TODAY.plusDays(5))));  // 진행 중

		ActiveCampaignResponse response = campaignService.getActiveCampaign(TODAY);

		assertThat(response.id()).isEqualTo(2L);
		assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
	}

	@Test
	void 진행_중인_게_없으면_가장_먼저_시작할_예정_캠페인() {
		given(campaignRepository.findAll()).willReturn(List.of(
				campaign(1L, TODAY.plusDays(30), TODAY.plusDays(50)),
				campaign(2L, TODAY.plusDays(3), TODAY.plusDays(20)),
				campaign(3L, TODAY.minusDays(30), TODAY.minusDays(1))));  // 끝남

		ActiveCampaignResponse response = campaignService.getActiveCampaign(TODAY);

		assertThat(response.id()).isEqualTo(2L);
		assertThat(response.status()).isEqualTo(CampaignStatus.PLANNED);
	}

	@Test
	void 끝난_캠페인만_있으면_null() {
		given(campaignRepository.findAll()).willReturn(List.of(campaign(1L, TODAY.minusDays(30), TODAY.minusDays(1))));

		assertThat(campaignService.getActiveCampaign(TODAY)).isNull();
	}

	@Test
	void 캠페인이_없으면_null() {
		given(campaignRepository.findAll()).willReturn(List.of());

		assertThat(campaignService.getActiveCampaign(TODAY)).isNull();
	}

	@Test
	void DB_status_컬럼은_무시하고_날짜로_판단한다() {
		Campaign forgotten = campaign(1L, TODAY.minusDays(30), TODAY.minusDays(1)); // 끝났는데 status는 ACTIVE
		given(campaignRepository.findAll()).willReturn(List.of(forgotten));

		assertThat(forgotten.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(campaignService.getActiveCampaign(TODAY)).isNull();
	}

	@Test
	void 운영진이_끝낸_캠페인은_기간이_남아_있어도_내려주지_않는다() {
		Campaign closed = campaign(1L, TODAY.minusDays(10), TODAY.plusDays(5));
		closed.end();
		given(campaignRepository.findAll()).willReturn(List.of(closed));

		assertThat(campaignService.getActiveCampaign(TODAY)).isNull();
	}

	@Test
	void 거래완료_물품_집계를_함께_내려준다() {
		given(campaignRepository.findAll()).willReturn(List.of(campaign(1L, TODAY.minusDays(1), TODAY.plusDays(1))));
		given(reservationRepository.summarizeCompletedInCampaign(eq(1L), any(), any())).willReturn(new CompletedItemSummary(128, 3420));

		ActiveCampaignResponse response = campaignService.getActiveCampaign(TODAY);

		assertThat(response.reusedCount()).isEqualTo(128);
		assertThat(response.carbonReductionKg()).isEqualTo(3420);
	}

	private static Campaign campaign(Long id, LocalDate start, LocalDate end) {
		Campaign campaign = Campaign.builder()
				.name("캠페인" + id)
				.registrationStartDate(start)
				.registrationEndDate(end)
				.applicationStartDate(start)
				.applicationEndDate(end)
				.pickupStartDate(start)
				.pickupEndDate(end)
				.status(CampaignStatus.ACTIVE)
				.build();
		ReflectionTestUtils.setField(campaign, "id", id);
		return campaign;
	}

}
