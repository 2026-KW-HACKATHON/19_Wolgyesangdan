package com.Wolgyesangdan.backend.domain.application.service;

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
	 * 물품 신청. 검증 순서: (1) 물품 존재 + OPEN 여부 (2) 동네 인증 승인 여부 (3) 연락 수단 설정 여부 (4) 중복 신청 여부.
	 * 통과하면 priority_score를 스냅샷으로 저장하고, 이 물품의 대기 신청 전체의 waitlist_rank를 다시 매긴다.
	 */
	@Transactional
	public ApplicationCreateResponse apply(Long userId, Long itemId) {
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
		if (item.getStatus() != ItemStatus.OPEN) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_ITEM_NOT_OPEN);
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
