package com.Wolgyesangdan.backend.domain.application.service;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.application.dto.ApplicationCreateResponse;
import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.exception.ApplicationErrorCode;
import com.Wolgyesangdan.backend.domain.application.repository.ApplicationRepository;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

	private final ApplicationRepository applicationRepository;
	private final ItemRepository itemRepository;
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
		if (applicationRepository.existsByItemIdAndApplicantId(itemId, userId)) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_ALREADY_EXISTS);
		}

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

	private void recalculateWaitlistRanks(Item item) {
		List<Application> waiting = applicationRepository
				.findByItemAndStatusOrderByPriorityScoreDescCreatedAtAscIdAsc(item, ApplicationStatus.WAITING);
		for (int i = 0; i < waiting.size(); i++) {
			waiting.get(i).assignWaitlistRank(i + 1);
		}
	}

}
