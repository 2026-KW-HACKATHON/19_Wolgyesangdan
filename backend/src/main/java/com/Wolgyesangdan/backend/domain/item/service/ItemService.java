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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.repository.ApplicationRepository;
import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
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
import com.Wolgyesangdan.backend.domain.item.entity.ItemType;
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
	 * 내가 등록한 물품 (마이페이지). 본인 목록이라 취소된 물품까지 상태와 관계없이 전부(삭제한 물품만 뺀다), 최근 등록순.
	 * 대표 사진과 진행 중인 예약(배정된 신청 id·전달 예정 일시)은 페이지에 담긴 물품 id로 한 번씩만 조회해서 붙인다 (N+1 방지).
	 */
	public Page<MyItemSummaryResponse> getMyItems(Long userId, int page, int size) {
		Page<Item> items = itemRepository.findByOwnerIdAndDeletedFalse(userId,
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
	 * 물품 상세 (비회원 허용). 상태와 관계없이 조회된다 — 상태는 응답의 status로 프론트가 표시. 삭제한 물품은 없는 물품으로 본다.
	 * viewerId(보는 사람, 비회원이면 null) 기준으로 내 물품인지·내 신청이 있는지를 함께 내려준다 (#168).
	 * 쿼리: 물품+등록자+캠페인 1번, 사진 1번, 거래 방식 1번, 등록자 전달 완료 횟수 1번
	 * (+ 등록자가 아닌 회원이 보면 내 신청 1번).
	 */
	public ItemDetailResponse getItem(Long itemId, Long viewerId) {
		Item item = itemRepository.findWithOwnerAndCampaignById(itemId)
				.filter(found -> !found.isDeleted())
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
		// 관리자가 숨긴 물품은 등록자 본인을 포함해 누구에게도 보여주지 않는다 (#214)
		if (item.isHidden()) {
			throw new BusinessException(ItemErrorCode.ITEM_HIDDEN);
		}
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
	 * - 거점 거래를 포함하면 전달 가능 기간(적었다면)도 캠페인 기간 안이어야 한다 (#256)
	 * - 직거래만이면 신청 마감은 등록일 + 3일 23:59:59
	 * - 등록 즉시 OPEN, 예상 탄소 절감량은 품목 값(#284), 품목이 없으면 대분류 기준표 값을 스냅샷으로 저장
	 * - 품목은 고른 대분류에 속해야 한다
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
		if (campaign != null) {
			validateAvailablePeriodInCampaign(request, campaign);
		}
		LocalDateTime applicationDeadline = campaign != null
				? campaign.getApplicationEndDate().atTime(END_OF_DAY)
				: today.plusDays(DIRECT_APPLICATION_DAYS).atTime(END_OF_DAY);
		ItemType itemType = request.itemType();
		int carbonReduction = carbonReductionOf(request);
		boolean defect = Boolean.TRUE.equals(request.defectYn());

		Item item = itemRepository.save(Item.builder()
				.owner(owner)
				.campaign(campaign)
				.name(request.name().strip())
				.categoryGroup(request.categoryGroup())
				.itemType(itemType)
				// 세부 카테고리는 품목 이름으로 채운다 — 품목이 없으면 요청에 온 값 그대로
				.category(itemType != null ? itemType.getLabel() : request.category())
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
		List<ItemImage> images = saveImagesAndTradeMethods(item, request.imageUrls(), tradeMethods);

		long givenCount = reservationRepository.countCompletedByItemOwnerId(owner.getId());
		return ItemDetailResponse.of(item, List.copyOf(tradeMethods), images, givenCount, true, null);
	}

	/**
	 * 물품 수정 — 등록자 본인이, 신청을 받는 중이고 아직 아무도 신청하지 않은 물품만 (Item.isModifiable).
	 * 본문은 등록과 같고 같은 규칙으로 검증한다. 사진·거래 방식은 보낸 목록으로 통째로 바꾸고,
	 * 탄소 절감량·신청 마감은 바뀐 값으로 다시 정한다.
	 * - 직거래만이면 마감은 처음 등록한 날 + 3일 23:59:59 — 수정해도 신청 기간이 늘어나지 않는다
	 * - 이미 연결된 캠페인으로 거점 거래를 유지하면 물품 등록 기간이 지났어도 수정할 수 있다 (등록 기간 안에 등록한 물품이라서).
	 *   새로 거점 거래를 고르거나 다른 캠페인으로 바꾸면 등록과 똑같이 등록 기간 안이어야 한다
	 */
	@Transactional
	public ItemDetailResponse updateItem(Long userId, Long itemId, ItemCreateRequest request) {
		Item item = findModifiableItem(userId, itemId);

		EnumSet<TradeMethod> tradeMethods = EnumSet.copyOf(request.tradeMethods());
		Campaign campaign = null;
		if (tradeMethods.contains(TradeMethod.CAMPAIGN)) {
			Campaign current = item.getCampaign();
			campaign = current != null && current.getId().equals(request.campaignId())
					? current
					: findCampaignAcceptingItems(request.campaignId(), LocalDate.now());
			validateAvailablePeriodInCampaign(request, campaign);
		}
		LocalDateTime applicationDeadline = campaign != null
				? campaign.getApplicationEndDate().atTime(END_OF_DAY)
				: item.getCreatedAt().toLocalDate().plusDays(DIRECT_APPLICATION_DAYS).atTime(END_OF_DAY);
		ItemType itemType = request.itemType();
		int carbonReduction = carbonReductionOf(request);
		boolean defect = Boolean.TRUE.equals(request.defectYn());

		item.update(campaign,
				request.name().strip(),
				request.categoryGroup(),
				itemType,
				itemType != null ? itemType.getLabel() : request.category(),
				request.description(),
				request.conditionGrade(),
				request.usagePeriod(),
				defect,
				defect ? request.defectDescription() : null,
				request.workingStatus(),
				request.size(),
				request.transportDifficulty(),
				carbonReduction,
				request.availableFrom(),
				request.availableUntil(),
				request.disposalDeadline(),
				applicationDeadline);
		itemImageRepository.deleteByItemId(itemId);
		itemTradeMethodRepository.deleteByItemId(itemId);
		List<ItemImage> images = saveImagesAndTradeMethods(item, request.imageUrls(), tradeMethods);

		long givenCount = reservationRepository.countCompletedByItemOwnerId(userId);
		return ItemDetailResponse.of(item, List.copyOf(tradeMethods), images, givenCount, true, null);
	}

	/**
	 * 물품 삭제 — 수정과 같은 조건(등록자 본인, 신청자 0명인 신청 받는 중 물품)일 때만.
	 * 취소한 신청 기록이 물품을 참조하고 있을 수 있어 행은 지우지 않고, 종료 상태 + 삭제 표시로 바꾼다 (Item.delete)
	 */
	@Transactional
	public void deleteItem(Long userId, Long itemId) {
		findModifiableItem(userId, itemId).delete();
	}

	// 수정·삭제할 물품. 물품 행을 잠그고 읽어서, 같은 물품에 동시에 들어온 신청(ApplicationService.apply도 같은 행을 잠근다)과
	// 한 줄씩 처리된다 — 신청자 0명을 확인한 뒤에 신청이 끼어들지 않는다
	private Item findModifiableItem(Long userId, Long itemId) {
		Item item = itemRepository.findByIdForUpdate(itemId)
				.filter(found -> !found.isDeleted())
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
		if (item.isHidden()) {
			throw new BusinessException(ItemErrorCode.ITEM_HIDDEN);
		}
		if (!item.getOwner().getId().equals(userId)) {
			throw new BusinessException(ItemErrorCode.ITEM_NOT_OWNER);
		}
		if (!item.isModifiable()) {
			throw new BusinessException(ItemErrorCode.ITEM_NOT_MODIFIABLE);
		}
		return item;
	}

	// 예상 탄소 절감량 — 품목 값(#284), 품목이 없으면 대분류 기준표 값. 품목은 고른 대분류에 속해야 한다
	private int carbonReductionOf(ItemCreateRequest request) {
		ItemType itemType = request.itemType();
		if (itemType != null && itemType.getCategoryGroup() != request.categoryGroup()) {
			throw new BusinessException(ItemErrorCode.ITEM_TYPE_CATEGORY_MISMATCH);
		}
		return itemType != null
				? itemType.getCarbonReductionKg()
				: categoryCarbonReferenceRepository.findByCategoryGroup(request.categoryGroup())
						.orElseThrow(() -> new IllegalStateException("탄소 참조값이 없는 카테고리: " + request.categoryGroup()))
						.getCarbonReductionKg();
	}

	// 사진은 보낸 순서대로(앞이 대표 사진) 저장한다
	private List<ItemImage> saveImagesAndTradeMethods(Item item, List<String> imageUrls, EnumSet<TradeMethod> tradeMethods) {
		List<ItemImage> images = itemImageRepository.saveAll(IntStream.range(0, imageUrls.size())
				.mapToObj(order -> ItemImage.builder()
						.item(item)
						.imageUrl(imageUrls.get(order))
						.displayOrder(order)
						.build())
				.toList());
		itemTradeMethodRepository.saveAll(tradeMethods.stream()
				.map(tradeMethod -> ItemTradeMethod.builder().item(item).tradeMethod(tradeMethod).build())
				.toList());
		return images;
	}

	// 물품별 대표 사진 = 순서가 가장 앞인 사진
	private Map<Long, String> findThumbnails(List<Long> itemIds) {
		return itemImageRepository.findByItemIdIn(itemIds).stream()
				.collect(Collectors.groupingBy(image -> image.getItem().getId(),
						Collectors.collectingAndThen(
								Collectors.minBy(Comparator.comparingInt(ItemImage::getDisplayOrder)),
								image -> image.map(ItemImage::getImageUrl).orElse(null))));
	}

	// 거점 거래는 캠페인 기간에만 진행되므로, 전달 가능 기간을 적었다면 시작·종료일 모두 캠페인 전체 기간 안이어야 한다 (#256).
	// "직거래 + 거점"을 함께 고른 물품도 배정되면 거점 거래로 진행되므로 같이 확인한다. 비워 둔 날짜는 확인하지 않는다
	private static void validateAvailablePeriodInCampaign(ItemCreateRequest request, Campaign campaign) {
		LocalDate start = campaign.periodStart();
		LocalDate end = campaign.periodEnd();
		boolean outside = Stream.of(request.availableFrom(), request.availableUntil())
				.filter(Objects::nonNull)
				.anyMatch(date -> date.isBefore(start) || date.isAfter(end));
		if (outside) {
			throw new BusinessException(ItemErrorCode.ITEM_TRADE_METHOD_INVALID,
					"거점 거래는 전달 가능 기간이 캠페인 기간(%d.%d ~ %d.%d) 안이어야 합니다.".formatted(
							start.getMonthValue(), start.getDayOfMonth(), end.getMonthValue(), end.getDayOfMonth()));
		}
	}

	// 거점 거래는 캠페인 물품 등록 기간(registration_start_date ~ registration_end_date) 안에만 고를 수 있다
	private Campaign findCampaignAcceptingItems(Long campaignId, LocalDate today) {
		if (campaignId == null) {
			throw new BusinessException(ItemErrorCode.ITEM_TRADE_METHOD_INVALID, "거점 거래를 선택하려면 캠페인을 지정해야 합니다.");
		}
		Campaign campaign = campaignRepository.findById(campaignId)
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_TRADE_METHOD_INVALID));
		// 운영진이 끝낸 캠페인은 등록 기간이 남아 있어도 받지 않는다 (#209)
		boolean acceptingItems = campaign.statusOn(today) != CampaignStatus.ENDED
				&& !today.isBefore(campaign.getRegistrationStartDate())
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
