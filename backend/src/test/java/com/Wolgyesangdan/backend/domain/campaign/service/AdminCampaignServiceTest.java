package com.Wolgyesangdan.backend.domain.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignCreateRequest;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignListResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignListResponse.PastCampaign;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignResponse;
import com.Wolgyesangdan.backend.domain.campaign.dto.AdminCampaignUpdateRequest;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.exception.CampaignErrorCode;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;
import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class AdminCampaignServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

	private final CampaignRepository campaignRepository = Mockito.mock(CampaignRepository.class);
	private final ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
	private final AdminCampaignService adminCampaignService = new AdminCampaignService(campaignRepository,
			new CampaignService(campaignRepository, reservationRepository));

	@BeforeEach
	void setUp() {
		given(reservationRepository.summarizeCompletedInCampaign(any(), any(), any())).willReturn(new CompletedItemSummary(0, 0));
		given(campaignRepository.save(any(Campaign.class))).willAnswer(invocation -> {
			Campaign saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 100L);
			return saved;
		});
	}

	// ── 목록 ──

	@Test
	void 진행_중인_캠페인은_current로_끝난_캠페인은_past로_내려준다() {
		Campaign running = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		Campaign endedLongAgo = campaign(2L, TODAY.minusDays(90), TODAY.minusDays(60));
		Campaign endedRecently = campaign(3L, TODAY.minusDays(40), TODAY.minusDays(20));
		given(campaignRepository.findAll()).willReturn(List.of(running, endedLongAgo, endedRecently));
		given(reservationRepository.summarizeCompletedInCampaign(eq(3L), any(), any())).willReturn(new CompletedItemSummary(128, 3420));

		AdminCampaignListResponse response = adminCampaignService.getCampaigns(TODAY);

		assertThat(response.current().id()).isEqualTo(1L);
		assertThat(response.current().status()).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(response.current().locationName()).isEqualTo("광운대 비마관 1층 거점");
		// 최근에 끝난 순
		assertThat(response.past()).extracting(PastCampaign::id).containsExactly(3L, 2L);
		PastCampaign recent = response.past().getFirst();
		assertThat(recent.name()).isEqualTo("캠페인3");
		assertThat(recent.startDate()).isEqualTo(TODAY.minusDays(40));
		assertThat(recent.endDate()).isEqualTo(TODAY.minusDays(20));
		assertThat(recent.reusedCount()).isEqualTo(128);
		assertThat(recent.carbonReductionKg()).isEqualTo(3420);
	}

	@Test
	void 예정_캠페인도_current로_내려준다() {
		given(campaignRepository.findAll()).willReturn(List.of(campaign(1L, TODAY.plusDays(3), TODAY.plusDays(20))));

		AdminCampaignListResponse response = adminCampaignService.getCampaigns(TODAY);

		assertThat(response.current().status()).isEqualTo(CampaignStatus.PLANNED);
		assertThat(response.past()).isEmpty();
	}

	@Test
	void 운영_중을_끈_캠페인은_기간이_남아_있어도_past다() {
		Campaign closed = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		closed.end();
		given(campaignRepository.findAll()).willReturn(List.of(closed));

		AdminCampaignListResponse response = adminCampaignService.getCampaigns(TODAY);

		assertThat(response.current()).isNull();
		assertThat(response.past()).extracting(PastCampaign::id).containsExactly(1L);
	}

	@Test
	void 캠페인이_없으면_current는_null이고_past는_빈_목록() {
		given(campaignRepository.findAll()).willReturn(List.of());

		AdminCampaignListResponse response = adminCampaignService.getCampaigns(TODAY);

		assertThat(response.current()).isNull();
		assertThat(response.past()).isEmpty();
	}

	// ── 생성 ──

	@Test
	void 캠페인을_만든다() {
		given(campaignRepository.findAll()).willReturn(List.of(campaign(1L, TODAY.minusDays(40), TODAY.minusDays(20))));

		AdminCampaignResponse response = adminCampaignService.createCampaign(new AdminCampaignCreateRequest(
				" 2026 가을 캠페인 ", "소개", TODAY.plusDays(1), TODAY.plusDays(10), TODAY.plusDays(5), TODAY.plusDays(12),
				TODAY.plusDays(5), TODAY.plusDays(14), "광운대 비마관 1층 거점", "서울 노원구 광운로 20", "  "), TODAY);

		ArgumentCaptor<Campaign> saved = ArgumentCaptor.forClass(Campaign.class);
		verify(campaignRepository).save(saved.capture());
		assertThat(saved.getValue().getName()).isEqualTo("2026 가을 캠페인");
		assertThat(saved.getValue().getHubHours()).isNull();
		// 아직 시작 전이라 예정
		assertThat(saved.getValue().getStatus()).isEqualTo(CampaignStatus.PLANNED);
		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.status()).isEqualTo(CampaignStatus.PLANNED);
		assertThat(response.pickupEndDate()).isEqualTo(TODAY.plusDays(14));
	}

	@Test
	void 오늘이_기간_안이면_진행_중으로_만든다() {
		given(campaignRepository.findAll()).willReturn(List.of());

		AdminCampaignResponse response = adminCampaignService.createCampaign(createRequest(TODAY, TODAY.plusDays(10)), TODAY);

		assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
	}

	@Test
	void 예정이거나_진행_중인_캠페인이_있으면_409_CAMPAIGN_ALREADY_RUNNING() {
		given(campaignRepository.findAll()).willReturn(List.of(campaign(1L, TODAY.plusDays(30), TODAY.plusDays(50))));

		assertThatThrownBy(() -> adminCampaignService.createCampaign(createRequest(TODAY, TODAY.plusDays(10)), TODAY))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(CampaignErrorCode.CAMPAIGN_ALREADY_RUNNING);
		verify(campaignRepository, never()).save(any());
	}

	@Test
	void 기간의_시작일이_종료일보다_늦으면_400_CAMPAIGN_PERIOD_INVALID() {
		given(campaignRepository.findAll()).willReturn(List.of());
		AdminCampaignCreateRequest request = new AdminCampaignCreateRequest("캠페인", null,
				TODAY, TODAY.plusDays(10), TODAY.plusDays(12), TODAY.plusDays(11), TODAY, TODAY.plusDays(14),
				"거점", "주소", null);

		assertThatThrownBy(() -> adminCampaignService.createCampaign(request, TODAY))
				.isInstanceOf(BusinessException.class)
				.hasMessage("신청 기간의 시작일은 종료일보다 늦을 수 없습니다.")
				.extracting("errorCode").isEqualTo(CampaignErrorCode.CAMPAIGN_PERIOD_INVALID);
		verify(campaignRepository, never()).save(any());
	}

	@Test
	void 시작일과_종료일이_같은_날이면_만들_수_있다() {
		given(campaignRepository.findAll()).willReturn(List.of());

		assertThat(adminCampaignService.createCampaign(createRequest(TODAY, TODAY), TODAY).id()).isEqualTo(100L);
	}

	// ── 수정 ──

	@Test
	void 보낸_필드만_바꾸고_나머지는_그대로_둔다() {
		Campaign campaign = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));

		AdminCampaignResponse response = adminCampaignService.updateCampaign(1L,
				update().name(" 새 이름 ").pickupEndDate(TODAY.plusDays(15)).build(), TODAY);

		assertThat(response.name()).isEqualTo("새 이름");
		assertThat(response.pickupEndDate()).isEqualTo(TODAY.plusDays(15));
		assertThat(response.description()).isEqualTo("소개");
		assertThat(response.registrationStartDate()).isEqualTo(TODAY.minusDays(3));
		assertThat(response.locationName()).isEqualTo("광운대 비마관 1층 거점");
		assertThat(response.hubHours()).isEqualTo("평일 10:00 ~ 18:00");
		assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
	}

	@Test
	void 소개_문구와_운영_시간은_빈_문자열을_보내면_지워진다() {
		Campaign campaign = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));

		AdminCampaignResponse response = adminCampaignService.updateCampaign(1L,
				update().description("").hubHours(" ").build(), TODAY);

		assertThat(response.description()).isNull();
		assertThat(response.hubHours()).isNull();
	}

	@Test
	void 운영_중을_끄면_ENDED가_된다() {
		Campaign campaign = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));

		AdminCampaignResponse response = adminCampaignService.updateCampaign(1L,
				update().status(CampaignStatus.ENDED).build(), TODAY);

		assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED);
		assertThat(response.status()).isEqualTo(CampaignStatus.ENDED);
	}

	@Test
	void 끈_캠페인을_다시_켠다() {
		Campaign closed = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		closed.end();
		given(campaignRepository.findById(1L)).willReturn(Optional.of(closed));
		given(campaignRepository.findAll()).willReturn(List.of(closed, campaign(2L, TODAY.minusDays(40), TODAY.minusDays(20))));

		AdminCampaignResponse response = adminCampaignService.updateCampaign(1L,
				update().status(CampaignStatus.ACTIVE).build(), TODAY);

		assertThat(closed.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
	}

	@Test
	void 다른_캠페인이_진행_중이면_끈_캠페인을_다시_켤_수_없다() {
		Campaign closed = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		closed.end();
		given(campaignRepository.findById(1L)).willReturn(Optional.of(closed));
		given(campaignRepository.findAll()).willReturn(List.of(closed, campaign(2L, TODAY.plusDays(1), TODAY.plusDays(20))));

		assertThatThrownBy(() -> adminCampaignService.updateCampaign(1L, update().status(CampaignStatus.ACTIVE).build(), TODAY))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(CampaignErrorCode.CAMPAIGN_ALREADY_RUNNING);
	}

	@Test
	void 다른_캠페인이_진행_중이면_끝난_캠페인의_기간을_늘려_되살릴_수_없다() {
		Campaign ended = campaign(1L, TODAY.minusDays(40), TODAY.minusDays(20));
		given(campaignRepository.findById(1L)).willReturn(Optional.of(ended));
		given(campaignRepository.findAll()).willReturn(List.of(ended, campaign(2L, TODAY.minusDays(1), TODAY.plusDays(20))));

		assertThatThrownBy(() -> adminCampaignService.updateCampaign(1L,
				update().pickupEndDate(TODAY.plusDays(5)).build(), TODAY))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(CampaignErrorCode.CAMPAIGN_ALREADY_RUNNING);
	}

	@Test
	void 끝난_캠페인의_이름만_고치는_건_다른_캠페인이_진행_중이어도_된다() {
		Campaign ended = campaign(1L, TODAY.minusDays(40), TODAY.minusDays(20));
		given(campaignRepository.findById(1L)).willReturn(Optional.of(ended));
		given(campaignRepository.findAll()).willReturn(List.of(ended, campaign(2L, TODAY.minusDays(1), TODAY.plusDays(20))));

		AdminCampaignResponse response = adminCampaignService.updateCampaign(1L, update().name("고친 이름").build(), TODAY);

		assertThat(response.name()).isEqualTo("고친 이름");
		assertThat(response.status()).isEqualTo(CampaignStatus.ENDED);
	}

	@Test
	void 수정한_결과_시작일이_종료일보다_늦으면_400_CAMPAIGN_PERIOD_INVALID() {
		Campaign campaign = campaign(1L, TODAY.minusDays(3), TODAY.plusDays(10));
		given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));

		// 종료일은 그대로(+10)인데 시작일만 그 뒤로 보낸다
		assertThatThrownBy(() -> adminCampaignService.updateCampaign(1L,
				update().registrationStartDate(TODAY.plusDays(11)).build(), TODAY))
				.isInstanceOf(BusinessException.class)
				.hasMessage("물품 등록 기간의 시작일은 종료일보다 늦을 수 없습니다.")
				.extracting("errorCode").isEqualTo(CampaignErrorCode.CAMPAIGN_PERIOD_INVALID);
	}

	@Test
	void 없는_캠페인이면_404_CAMPAIGN_NOT_FOUND() {
		given(campaignRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> adminCampaignService.updateCampaign(999L, update().name("이름").build(), TODAY))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(CampaignErrorCode.CAMPAIGN_NOT_FOUND);
	}

	private static AdminCampaignCreateRequest createRequest(LocalDate start, LocalDate end) {
		return new AdminCampaignCreateRequest("캠페인", null, start, end, start, end, start, end, "거점", "주소", null);
	}

	private static UpdateRequestBuilder update() {
		return new UpdateRequestBuilder();
	}

	private static Campaign campaign(Long id, LocalDate start, LocalDate end) {
		Campaign campaign = Campaign.builder()
				.name("캠페인" + id)
				.description("소개")
				.registrationStartDate(start)
				.registrationEndDate(end)
				.applicationStartDate(start)
				.applicationEndDate(end)
				.pickupStartDate(start)
				.pickupEndDate(end)
				.locationName("광운대 비마관 1층 거점")
				.locationAddress("서울 노원구 광운로 20")
				.hubHours("평일 10:00 ~ 18:00")
				.status(CampaignStatus.ACTIVE)
				.build();
		ReflectionTestUtils.setField(campaign, "id", id);
		return campaign;
	}

	/** 수정 요청은 필드가 12개라, 테스트에서 바꿀 필드만 적도록 감싼다 */
	private static class UpdateRequestBuilder {

		private String name;
		private String description;
		private LocalDate registrationStartDate;
		private LocalDate pickupEndDate;
		private String hubHours;
		private CampaignStatus status;

		UpdateRequestBuilder name(String name) {
			this.name = name;
			return this;
		}

		UpdateRequestBuilder description(String description) {
			this.description = description;
			return this;
		}

		UpdateRequestBuilder registrationStartDate(LocalDate registrationStartDate) {
			this.registrationStartDate = registrationStartDate;
			return this;
		}

		UpdateRequestBuilder pickupEndDate(LocalDate pickupEndDate) {
			this.pickupEndDate = pickupEndDate;
			return this;
		}

		UpdateRequestBuilder hubHours(String hubHours) {
			this.hubHours = hubHours;
			return this;
		}

		UpdateRequestBuilder status(CampaignStatus status) {
			this.status = status;
			return this;
		}

		AdminCampaignUpdateRequest build() {
			return new AdminCampaignUpdateRequest(name, description, registrationStartDate, null, null, null, null,
					pickupEndDate, null, null, hubHours, status);
		}

	}

}
