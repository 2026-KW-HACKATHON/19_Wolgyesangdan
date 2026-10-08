package com.Wolgyesangdan.backend.domain.item.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemType;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;

/**
 * 물품 상세. 등록자의 연락처는 담지 않는다 — 배정된 상대에게만 예약 상세 API로 공개.
 * isMine·myApplication은 보는 사람 기준 값이다 (비회원이면 false·null, #168).
 * itemType·carbonBasis는 품목을 고른 물품만 값이 있다 (#284). null이면 대분류 값으로 계산한 물품.
 */
public record ItemDetailResponse(
		Long id,
		String name,
		String category,
		CategoryGroup categoryGroup,
		ItemType itemType,
		String description,
		String conditionGrade,
		String usagePeriod,
		boolean defectYn,
		String defectDescription,
		String workingStatus,
		String size,
		String transportDifficulty,
		int estimatedCarbonReduction,
		String carbonBasis,
		LocalDate availableFrom,
		LocalDate availableUntil,
		LocalDate disposalDeadline,
		LocalDateTime applicationDeadline,
		ItemStatus status,
		int applicantCount,
		int maxApplicants,
		List<TradeMethod> tradeMethods,
		List<ImageResponse> images,
		CampaignInfo campaign,
		OwnerInfo owner,
		boolean isMine,
		MyApplication myApplication) {

	public static ItemDetailResponse of(Item item, List<TradeMethod> tradeMethods, List<ItemImage> images,
			long ownerGivenCount, boolean isMine, MyApplication myApplication) {
		return new ItemDetailResponse(
				item.getId(),
				item.getName(),
				item.getCategory(),
				item.getCategoryGroup(),
				item.getItemType(),
				item.getDescription(),
				item.getConditionGrade(),
				item.getUsagePeriod(),
				item.isDefectYn(),
				item.getDefectDescription(),
				item.getWorkingStatus(),
				item.getSize(),
				item.getTransportDifficulty(),
				item.getEstimatedCarbonReduction(),
				item.getItemType() == null ? null : item.getItemType().getBasis(),
				item.getAvailableFrom(),
				item.getAvailableUntil(),
				item.getDisposalDeadline(),
				item.getApplicationDeadline(),
				item.getStatus(),
				item.getApplicantCount(),
				Item.MAX_APPLICANTS,
				tradeMethods,
				images.stream().map(ImageResponse::from).toList(),
				item.getCampaign() == null ? null : CampaignInfo.from(item.getCampaign()),
				new OwnerInfo(item.getOwner().getNickname(), Math.toIntExact(ownerGivenCount)),
				isMine,
				myApplication);
	}

	public record ImageResponse(String imageUrl, int displayOrder) {

		static ImageResponse from(ItemImage image) {
			return new ImageResponse(image.getImageUrl(), image.getDisplayOrder());
		}
	}

	/** 거점 거래를 지원하는 물품만 값이 있다 */
	public record CampaignInfo(Long id, String locationName, String locationAddress, String hubHours) {

		static CampaignInfo from(Campaign campaign) {
			return new CampaignInfo(campaign.getId(), campaign.getLocationName(), campaign.getLocationAddress(),
					campaign.getHubHours());
		}
	}

	public record OwnerInfo(String nickname, int givenCount) {
	}

	/**
	 * 보는 사람이 이 물품에 넣은 신청 — 신청한 적 없거나 취소했으면 null.
	 * waitlistRank는 물품이 아직 배정 전이면 null이다 — 순번은 배정이 끝난 뒤에만 공개한다 (#265).
	 */
	public record MyApplication(Long id, ApplicationStatus status, Integer waitlistRank) {

		public static MyApplication from(Application application) {
			return new MyApplication(application.getId(), application.getStatus(), application.visibleWaitlistRank());
		}
	}

}
