package com.Wolgyesangdan.backend.domain.verification.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationService {

	// 제출 시각이 같으면 나중에 만들어진(id가 큰) 건이 최근 건
	private static final Comparator<PriorityVerification> BY_RECENCY = Comparator
			.comparing(PriorityVerification::getSubmittedAt)
			.thenComparing(PriorityVerification::getId);

	private final PriorityVerificationRepository priorityVerificationRepository;

	/**
	 * 내 인증 상태. 한 번이라도 신청한 유형만, 유형마다 가장 최근 제출 건 기준으로 내려준다 (RESIDENT → FRESHMAN → LOW_INCOME 순).
	 * 상태는 조회 시점 기준이라 만료일이 지난 승인은 EXPIRED로 나간다 (PriorityVerification.statusAt).
	 * 신청한 적 없는 유형은 아예 빠진다 (프론트가 "미신청"으로 처리).
	 */
	public List<MyVerificationResponse> getMyVerifications(Long userId) {
		return getMyVerifications(userId, LocalDateTime.now());
	}

	List<MyVerificationResponse> getMyVerifications(Long userId, LocalDateTime now) {
		return findLatestByType(userId).stream()
				.map(verification -> MyVerificationResponse.from(verification, now))
				.toList();
	}

	/**
	 * 유형별 가장 최근 제출 건 (RESIDENT → FRESHMAN → LOW_INCOME 순). 물품 신청 자격 체크도 이 조회를 쓴다.
	 * 승인 여부는 getStatus()가 아니라 statusAt(now)로 판단해야 만료된 인증이 통과하지 않는다.
	 */
	public Collection<PriorityVerification> findLatestByType(Long userId) {
		// 한 사용자의 인증 신청은 유형별로 몇 건뿐이라 전부 읽어서 고른다
		return priorityVerificationRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(PriorityVerification::getVerificationType, verification -> verification,
						BinaryOperator.maxBy(BY_RECENCY), () -> new EnumMap<>(VerificationType.class)))
				.values();
	}

}
