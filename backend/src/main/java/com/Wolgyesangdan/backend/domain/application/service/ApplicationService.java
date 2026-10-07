package com.Wolgyesangdan.backend.domain.application.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.Wolgyesangdan.backend.domain.application.dto.ApplicationCreateResponse;
import com.Wolgyesangdan.backend.domain.application.dto.MyApplicationSummaryResponse;
import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.exception.ApplicationErrorCode;
import com.Wolgyesangdan.backend.domain.application.repository.ApplicationRepository;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.item.repository.ItemImageRepository;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.reservation.repository.ReservationRepository;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

	private final ApplicationRepository applicationRepository;
	private final ItemRepository itemRepository;
	private final ItemImageRepository itemImageRepository;
	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final VerificationService verificationService;

	/**
	 * 물품 신청. 검증 순서: (1) 물품 존재 + OPEN 여부 + 신청 마감 전인지 (2) 본인 물품이 아닌지
	 * (3) 동네 인증 승인 여부 (4) 연락 수단 설정 여부 (5) 중복 신청 여부.
	 * 통과하면 priority_score를 스냅샷으로 저장하고, 이 물품의 대기 신청 전체의 waitlist_rank를 다시 매긴다.
	 *
	 * 물품 행을 잠그고 읽어서(findByIdForUpdate), 같은 물품에 동시에 신청해도 한 줄씩 순서대로 처리된다 —
	 * 정원을 넘겨 저장되거나 applicantCount가 누락되는 일이 없다.
	 */
	@Transactional
	public ApplicationCreateResponse apply(Long userId, Long itemId) {
		Item item = itemRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
		boolean deadlinePassed = item.getApplicationDeadline().isBefore(LocalDateTime.now());
		if (item.getStatus() != ItemStatus.OPEN || deadlinePassed) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_ITEM_NOT_OPEN);
		}
		if (item.getOwner().getId().equals(userId)) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_OWN_ITEM);
		}

		User applicant = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

		if (!verificationService.hasNeighborhoodVerification(userId)) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_NOT_ELIGIBLE);
		}
		if (applicant.getContactType() == null) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_CONTACT_NOT_SET);
		}
		applicationRepository.findByItemIdAndApplicantId(itemId, userId).ifPresent(this::removeCanceledForReapply);

		Application application = applicationRepository.save(Application.builder()
				.item(item)
				.applicant(applicant)
				.priorityScore(verificationService.calculatePriorityScore(userId))
				.status(ApplicationStatus.WAITING)
				.build());
		item.increaseApplicantCount();
		recalculateWaitlistRanks(item);

		return ApplicationCreateResponse.from(application);
	}

	/**
	 * 취소한 신청이면 지워서 다시 신청할 수 있게 한다 (#169). 행을 새로 만들어야 신청 시각(createdAt)이 지금으로
	 * 찍혀 대기 순서가 맨 뒤가 된다 — createdAt은 수정할 수 없는 컬럼이라 기존 행을 되살리지 않는다.
	 * 취소하지 않은 신청이나, 노쇼로 빠져 예약이 남아 있는 신청은 지금처럼 "이미 신청함"이다.
	 */
	private void removeCanceledForReapply(Application previous) {
		if (previous.getStatus() != ApplicationStatus.CANCELED
				|| reservationRepository.existsByApplicationId(previous.getId())) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_ALREADY_EXISTS);
		}
		applicationRepository.delete(previous);
		// Hibernate는 flush 때 insert를 delete보다 먼저 보내서, 바로 flush하지 않으면 새 신청 insert가 유니크 제약에 걸린다
		applicationRepository.flush();
	}

	/**
	 * 신청 취소. WAITING 상태일 때만 가능하고, 이미 SELECTED(이상)된 신청은 취소할 수 없다
	 * (배정 취소는 예약 도메인의 별도 흐름 — #89).
	 *
	 * 물품을 먼저 잠그고(findByIdForUpdate) 그 다음에 신청을 읽어야 한다 — 신청을 먼저 읽으면 영속성
	 * 컨텍스트에 캐시돼서, 잠금을 기다리는 동안(배정 스케줄러 #92가 먼저 SELECTED로 바꿀 수 있음) 바뀐
	 * 최신 상태가 아니라 잠그기 전에 읽은 낡은 상태로 검증하게 된다.
	 */
	@Transactional
	public void cancel(Long userId, Long applicationId) {
		Long itemId = applicationRepository.findItemIdById(applicationId)
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
		Item item = itemRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
		Application application = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));

		if (!application.getApplicant().getId().equals(userId)) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_NOT_OWNER);
		}
		if (application.getStatus() == ApplicationStatus.CANCELED) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_ALREADY_CANCELED);
		}
		if (application.getStatus() != ApplicationStatus.WAITING) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_ALREADY_SELECTED);
		}

		application.cancel();
		item.decreaseApplicantCount(LocalDateTime.now());
		recalculateWaitlistRanks(item);
	}

	/**
	 * 마이페이지 "내가 신청한 물품" — 최근 신청순. 대표 사진은 페이지에 담긴 물품 id로 한 번씩만 조회해서 붙인다 (N+1 방지).
	 */
	public Page<MyApplicationSummaryResponse> getMyApplications(Long userId, int page, int size) {
		Page<Application> applications = applicationRepository.findByApplicantId(userId,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		List<Long> itemIds = applications.map(application -> application.getItem().getId()).getContent();
		if (itemIds.isEmpty()) {
			return applications.map(application -> MyApplicationSummaryResponse.of(application, null));
		}

		Map<Long, String> thumbnails = findThumbnails(itemIds);
		return applications.map(application -> MyApplicationSummaryResponse.of(application,
				thumbnails.get(application.getItem().getId())));
	}

	private Map<Long, String> findThumbnails(List<Long> itemIds) {
		return itemImageRepository.findByItemIdIn(itemIds).stream()
				.collect(Collectors.groupingBy(image -> image.getItem().getId(),
						Collectors.collectingAndThen(
								Collectors.minBy(Comparator.comparingInt(ItemImage::getDisplayOrder)),
								image -> image.map(ItemImage::getImageUrl).orElse(null))));
	}

	private void recalculateWaitlistRanks(Item item) {
		List<Application> waiting = applicationRepository
				.findByItemAndStatusOrderByPriorityScoreDescCreatedAtAscIdAsc(item, ApplicationStatus.WAITING);
		for (int i = 0; i < waiting.size(); i++) {
			waiting.get(i).assignWaitlistRank(i + 1);
		}
	}

}
