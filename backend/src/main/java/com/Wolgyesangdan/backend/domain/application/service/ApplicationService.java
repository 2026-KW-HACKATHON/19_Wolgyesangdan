package com.Wolgyesangdan.backend.domain.application.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
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

	/** 신입생·기초수급자 중 하나라도 승인돼 있으면 1순위 그룹으로 취급한다 (주민 인증에 추가로 받는 인증, #59) */
	private static final Set<VerificationType> PRIORITY_TYPES = EnumSet.of(VerificationType.FRESHMAN,
			VerificationType.LOW_INCOME);
	private static final int PRIORITY_SCORE = 1;
	private static final int DEFAULT_SCORE = 0;

	private final ApplicationRepository applicationRepository;
	private final ItemRepository itemRepository;
	private final UserRepository userRepository;
	private final VerificationService verificationService;

	/**
	 * 물품 신청. 검증 순서: (1) 물품 존재 + OPEN 여부 (2) 주민 인증 승인 여부 (3) 연락 수단 설정 여부 (4) 중복 신청 여부.
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

		int priorityScore = checkEligibilityAndScorePriority(userId);

		if (applicant.getContactType() == null) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_CONTACT_NOT_SET);
		}
		if (applicationRepository.existsByItemIdAndApplicantId(itemId, userId)) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_ALREADY_EXISTS);
		}

		Application application = applicationRepository.save(Application.builder()
				.item(item)
				.applicant(applicant)
				.priorityScore(priorityScore)
				.status(ApplicationStatus.WAITING)
				.build());
		item.increaseApplicantCount();
		recalculateWaitlistRanks(item);

		return ApplicationCreateResponse.from(application);
	}

	// 주민 인증이 승인 상태가 아니면 신청 자체가 불가능하다. 신입생/기초수급자 인증이 추가로 승인돼 있으면 우선순위 점수를 준다.
	private int checkEligibilityAndScorePriority(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		Map<VerificationType, VerificationStatus> statusByType = verificationService.findLatestByType(userId).stream()
				.collect(Collectors.toMap(PriorityVerification::getVerificationType,
						verification -> verification.statusAt(now), (a, b) -> a,
						() -> new EnumMap<>(VerificationType.class)));
		Function<VerificationType, Boolean> isApproved = type -> statusByType.get(type) == VerificationStatus.APPROVED;

		if (!isApproved.apply(VerificationType.RESIDENT)) {
			throw new BusinessException(ApplicationErrorCode.APPLICATION_NOT_ELIGIBLE);
		}
		boolean hasPriority = PRIORITY_TYPES.stream().anyMatch(isApproved::apply);
		return hasPriority ? PRIORITY_SCORE : DEFAULT_SCORE;
	}

	private void recalculateWaitlistRanks(Item item) {
		List<Application> waiting = applicationRepository
				.findByItemAndStatusOrderByPriorityScoreDescCreatedAtAscIdAsc(item, ApplicationStatus.WAITING);
		for (int i = 0; i < waiting.size(); i++) {
			waiting.get(i).assignWaitlistRank(i + 1);
		}
	}

}
