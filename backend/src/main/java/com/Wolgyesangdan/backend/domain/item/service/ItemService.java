package com.Wolgyesangdan.backend.domain.item.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.stream.IntStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.repository.ApplicationRepository;
import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;
import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemCreateRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ItemDetailResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSearchCondition;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.MyItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemImageRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemSpecifications;
import com.Wolgyesangdan.backend.domain.item.repository.ItemTradeMethodRepository;

import com.Wolgyesangdan.backend.domain.reservation.dto.ItemSchedule;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

	/** 목록에 보여주는 상태 — 취소(CANCELED)와 신청 기간 전(REGISTERED)은 제외 */
	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;
	private final ItemTradeMethodRepository itemTradeMethodRepository;
	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository;
	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final CampaignRepository campaignRepository;
	private final ApplicationRepository applicationRepository;

	/** 전달 예정 일시를 보여주는 상태 — 배정 확정 이후 */
	private static final EnumSet<ItemStatus> SCHEDULED_STATUSES = EnumSet.of(ItemStatus.ASSIGNED, ItemStatus.COMPLETED);

	/** 직거래만 하는 물품의 신청 기간 (등록일 + N일 23:59:59 마감) */
	static final int DIRECT_APPLICATION_DAYS = 3;
	private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

	/**
	 * 물품 목록. 검색·필터·정렬은 ItemSpecifications에서 처리한다.
	 * 대표 사진과 거래 방식은 물품마다 따로 조회하지 않고 페이지에 담긴 물품 id로 한 번씩만 조회해서 붙인다 (N+1 방지).
	 */
	public Page<ItemSummaryResponse> getItems(ItemSearchCondition condition, int page, int size) {
		Page<Item> items = itemRepository.findAll(ItemSpecifications.search(condition.availability().statuses(), condition),
				PageRequest.of(page, size));
		List<Long> itemIds = items.map(Item::getId).getContent();
		if (itemIds.isEmpty()) {
			return items.map(item -> ItemSummaryResponse.of(item, null, List.of()));
		}

		Map<Long, String> thumbnails = findThumbnails(itemIds);
		Map<Long, List<TradeMethod>> tradeMethods = itemTradeMethodRepository.findByItemIdIn(itemIds).stream()
				.collect(Collectors.groupingBy(tradeMethod -> tradeMethod.getItem().getId(),
						Collectors.mapping(ItemTradeMethod::getTradeMethod,
								Collectors.collectingAndThen(Collectors.toList(),
										list -> list.stream().sorted().toList()))));

		return items.map(item -> ItemSummaryResponse.of(item,
				thumbnails.get(item.getId()),
				tradeMethods.getOrDefault(item.getId(), List.of())));
	}

	/**
	 * 내가 등록한 물품 (마이페이지). 본인 목록이라 취소된 물품까지 상태와 관계없이 전부, 최근 등록순.
	 * 대표 사진과 진행 중인 예약(배정된 신청 id·전달 예정 일시)은 페이지에 담긴 물품 id로 한 번씩만 조회해서 붙인다 (N+1 방지).
	 */
	public Page<MyItemSummaryResponse> getMyItems(Long userId, int page, int size) {
		Page<Item> items = itemRepository.findByOwnerId(userId,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		List<Long> itemIds = items.map(Item::getId).getContent();
		if (itemIds.isEmpty()) {
			return items.map(item -> MyItemSummaryResponse.of(item, null, null, null));
		}

		Map<Long, String> thumbnails = findThumbnails(itemIds);
		// 노쇼 승계로 진행 중인 예약이 여러 건이면 나중 건(id 순으로 뒤)을 쓴다
		Map<Long, ItemSchedule> schedules = new HashMap<>();
		reservationRepository.findActiveSchedulesByItemIdIn(itemIds)
				.forEach(schedule -> schedules.put(schedule.itemId(), schedule));

		return items.map(item -> {
			// 배정 확정 전(모집 중·마감)이나 취소된 물품은 예약 정보를 내려주지 않는다
			ItemSchedule schedule = SCHEDULED_STATUSES.contains(item.getStatus()) ? schedules.get(item.getId()) : null;
			return MyItemSummaryResponse.of(item,
					thumbnails.get(item.getId()),
					schedule == null ? null : schedule.applicationId(),
					schedule == null ? null : schedule.scheduledAt());
		});
	}

	/**
	 * 물품 상세 (비회원 허용). 상태와 관계없이 조회된다 — 상태는 응답의 status로 프론트가 표시.
	 * viewerId(보는 사람, 비회원이면 null) 기준으로 내 물품인지·내 신청이 있는지를 함께 내려준다 (#168).
	 * 쿼리: 물품+등록자+캠페인 1번, 사진 1번, 거래 방식 1번, 등록자 전달 완료 횟수 1번
	 * (+ 등록자가 아닌 회원이 보면 내 신청 1번).
	 */
	public ItemDetailResponse getItem(Long itemId, Long viewerId) {
		Item item = itemRepository.findWithOwnerAndCampaignById(itemId)
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
		List<TradeMethod> tradeMethods = itemTradeMethodRepository.findByItemId(itemId).stream()
				.map(ItemTradeMethod::getTradeMethod)
				.sorted()
				.toList();
		List<ItemImage> images = itemImageRepository.findByItemIdOrderByDisplayOrderAsc(itemId);
		long givenCount = reservationRepository.countCompletedByItemOwnerId(item.getOwner().getId());
		boolean mine = viewerId != null && viewerId.equals(item.getOwner().getId());
		// 등록자는 자기 물품에 신청할 수 없으니 찾지 않는다. 취소한 신청은 다시 신청할 수 있어서(#169) 없는 것으로 본다
		ItemDetailResponse.MyApplication myApplication = viewerId == null || mine ? null
				: applicationRepository.findByItemIdAndApplicantId(itemId, viewerId)
						.filter(application -> application.getStatus() != ApplicationStatus.CANCELED)
						.map(ItemDetailResponse.MyApplication::from)
						.orElse(null);
		return ItemDetailResponse.of(item, tradeMethods, images, givenCount, mine, myApplication);
	}

	/**
	 * 물품 등록 (2026-10-05 결정, #56)
	 * - 연락 수단을 설정하지 않았으면 등록 불가
	 * - 거점 거래를 포함하면 캠페인 물품 등록 기간 안이어야 하고, 신청 마감은 캠페인 신청 종료일 23:59:59
	 * - 직거래만이면 신청 마감은 등록일 + 3일 23:59:59
	 * - 등록 즉시 OPEN, 예상 탄소 절감량은 카테고리 기준표 값을 스냅샷으로 저장
	 */
	@Transactional
	public ItemDetailResponse createItem(Long userId, ItemCreateRequest request) {
		User owner = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_USER_NOT_FOUND));
		if (owner.getContactType() == null) {
			throw new BusinessException(ItemErrorCode.ITEM_CONTACT_NOT_SET);
		}

		LocalDate today = LocalDate.now();
		EnumSet<TradeMethod> tradeMethods = EnumSet.copyOf(request.tradeMethods());
		Campaign campaign = tradeMethods.contains(TradeMethod.CAMPAIGN)
				? findCampaignAcceptingItems(request.campaignId(), today)
				: null;
		LocalDateTime applicationDeadline = campaign != null
				? campaign.getApplicationEndDate().atTime(END_OF_DAY)
				: today.plusDays(DIRECT_APPLICATION_DAYS).atTime(END_OF_DAY);
		int carbonReduction = categoryCarbonReferenceRepository.findByCategoryGroup(request.categoryGroup())
				.orElseThrow(() -> new IllegalStateException("탄소 참조값이 없는 카테고리: " + request.categoryGroup()))
				.getCarbonReductionKg();
		boolean defect = Boolean.TRUE.equals(request.defectYn());

		Item item = itemRepository.save(Item.builder()
				.owner(owner)
				.campaign(campaign)
				.name(request.name().strip())
				.categoryGroup(request.categoryGroup())
				.category(request.category())
				.description(request.description())
				.conditionGrade(request.conditionGrade())
				.usagePeriod(request.usagePeriod())
				.defectYn(defect)
				.defectDescription(defect ? request.defectDescription() : null)
				.workingStatus(request.workingStatus())
				.size(request.size())
				.transportDifficulty(request.transportDifficulty())
				.estimatedCarbonReduction(carbonReduction)
				.availableFrom(request.availableFrom())
				.availableUntil(request.availableUntil())
				.disposalDeadline(request.disposalDeadline())
				.applicationDeadline(applicationDeadline)
				.status(ItemStatus.OPEN)
				.build());
		List<ItemImage> images = itemImageRepository.saveAll(IntStream.range(0, request.imageUrls().size())
				.mapToObj(order -> ItemImage.builder()
						.item(item)
						.imageUrl(request.imageUrls().get(order))
						.displayOrder(order)
						.build())
				.toList());
		itemTradeMethodRepository.saveAll(tradeMethods.stream()
				.map(tradeMethod -> ItemTradeMethod.builder().item(item).tradeMethod(tradeMethod).build())
				.toList());

		long givenCount = reservationRepository.countCompletedByItemOwnerId(owner.getId());
		return ItemDetailResponse.of(item, List.copyOf(tradeMethods), images, givenCount, true, null);
	}

	// 물품별 대표 사진 = 순서가 가장 앞인 사진
	private Map<Long, String> findThumbnails(List<Long> itemIds) {
		return itemImageRepository.findByItemIdIn(itemIds).stream()
				.collect(Collectors.groupingBy(image -> image.getItem().getId(),
						Collectors.collectingAndThen(
								Collectors.minBy(Comparator.comparingInt(ItemImage::getDisplayOrder)),
								image -> image.map(ItemImage::getImageUrl).orElse(null))));
	}

	// 거점 거래는 캠페인 물품 등록 기간(registration_start_date ~ registration_end_date) 안에만 고를 수 있다
	private Campaign findCampaignAcceptingItems(Long campaignId, LocalDate today) {
		if (campaignId == null) {
			throw new BusinessException(ItemErrorCode.ITEM_TRADE_METHOD_INVALID, "거점 거래를 선택하려면 캠페인을 지정해야 합니다.");
		}
		Campaign campaign = campaignRepository.findById(campaignId)
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_TRADE_METHOD_INVALID));
		boolean acceptingItems = !today.isBefore(campaign.getRegistrationStartDate())
				&& !today.isAfter(campaign.getRegistrationEndDate())
				&& !today.isAfter(campaign.getApplicationEndDate());
		if (!acceptingItems) {
			throw new BusinessException(ItemErrorCode.ITEM_TRADE_METHOD_INVALID);
		}
		return campaign;
	}

	/**
	 * 카테고리 대분류별 예상 탄소 절감량. 순서는 CategoryGroup 선언 순서(가구·가전·주방·생활·기타)로 고정 —
	 * 프론트 필터 칩/등록 폼이 이 순서 그대로 그린다.
	 */
	public List<CategoryResponse> getCategories() {
		return categoryCarbonReferenceRepository.findAll().stream()
				.sorted(Comparator.comparing(CategoryCarbonReference::getCategoryGroup))
				.map(CategoryResponse::from)
				.toList();
	}

}
