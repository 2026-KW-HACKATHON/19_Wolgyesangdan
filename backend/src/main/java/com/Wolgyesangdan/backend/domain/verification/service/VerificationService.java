package com.Wolgyesangdan.backend.domain.verification.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

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
	private final UserRepository userRepository;

	/**
	 * 우선배정 인증 신청 (신입생·기초수급자). 항상 PENDING으로 접수하고, 관리자가 서류를 확인해 승인·반려한다.
	 * 같은 유형에 심사 중인 신청이나 유효한 승인이 있으면 거절한다 (반려·만료된 뒤에는 다시 신청 가능).
	 */
	@Transactional
	public VerificationCreateResponse createVerification(Long userId, VerificationCreateRequest request) {
		return createVerification(userId, request, LocalDateTime.now());
	}

	VerificationCreateResponse createVerification(Long userId, VerificationCreateRequest request, LocalDateTime now) {
		VerificationType type = request.verificationType();
		PriorityVerification verification = PriorityVerification.builder()
				.verificationType(type)
				.documentType(request.documentType())
				.fileKey(request.fileKey())
				.applicantName(request.applicantName().strip())
				.status(VerificationStatus.PENDING)
				.submittedAt(now)
				.user(findUser(userId))
				.build();
		if (priorityVerificationRepository.existsByUserIdAndVerificationTypeAndStatus(userId, type,
				VerificationStatus.PENDING)) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_PENDING);
		}
		// 유효한 승인이 있는데 다시 신청하면 새 PENDING 건이 최근 건이 돼서 신청 자격을 잃는다
		boolean alreadyApproved = findLatestByType(userId).stream()
				.anyMatch(latest -> latest.getVerificationType() == type
						&& latest.statusAt(now) == VerificationStatus.APPROVED);
		if (alreadyApproved) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
		}
		return VerificationCreateResponse.from(priorityVerificationRepository.save(verification));
	}

	/**
	 * GPS 동네 인증. 월계1동 안인지는 프론트가 판정하고(2026-10-05 결정, #73), 서버는 심사 없이 바로 승인으로 기록한다.
	 * 위치 좌표·주소는 받지도 저장하지도 않는다. 이미 유효한 동네 인증이 있으면 거절한다.
	 */
	@Transactional
	public VerificationCreateResponse verifyNeighborhood(Long userId) {
		return verifyNeighborhood(userId, LocalDateTime.now());
	}

	VerificationCreateResponse verifyNeighborhood(Long userId, LocalDateTime now) {
		User user = findUser(userId);
		if (hasNeighborhoodVerification(userId, now)) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
		}
		return VerificationCreateResponse.from(priorityVerificationRepository.save(PriorityVerification.builder()
				.user(user)
				.verificationType(VerificationType.NEIGHBORHOOD)
				.status(VerificationStatus.APPROVED)
				.submittedAt(now)
				.build()));
	}

	/**
	 * 내 인증 상태. 한 번이라도 신청한 유형만, 유형마다 가장 최근 제출 건 기준으로 내려준다 (NEIGHBORHOOD → FRESHMAN → LOW_INCOME 순).
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
	 * 유형별 가장 최근 제출 건 (NEIGHBORHOOD → FRESHMAN → LOW_INCOME 순). 신청 자격·우선배정 점수도 이 조회를 쓴다.
	 * 승인 여부는 getStatus()가 아니라 statusAt(now)로 판단해야 만료된 인증이 통과하지 않는다.
	 */
	public Collection<PriorityVerification> findLatestByType(Long userId) {
		// 한 사용자의 인증 신청은 유형별로 몇 건뿐이라 전부 읽어서 고른다
		return priorityVerificationRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(PriorityVerification::getVerificationType, verification -> verification,
						BinaryOperator.maxBy(BY_RECENCY), () -> new EnumMap<>(VerificationType.class)))
				.values();
	}

	/** 나눔 신청 자격 — 동네 인증이 유효하게 승인돼 있는지 (물품 신청에서 사용) */
	public boolean hasNeighborhoodVerification(Long userId) {
		return hasNeighborhoodVerification(userId, LocalDateTime.now());
	}

	boolean hasNeighborhoodVerification(Long userId, LocalDateTime now) {
		return findLatestByType(userId).stream()
				.anyMatch(latest -> latest.getVerificationType() == VerificationType.NEIGHBORHOOD
						&& latest.statusAt(now) == VerificationStatus.APPROVED);
	}

	/**
	 * 우선배정 점수 — 신입생·기초수급자 중 하나라도 유효하게 승인돼 있으면 1, 아니면 0 (가산점 중복 없음).
	 * 물품 신청 시점에 계산해 Application.priorityScore에 저장한다.
	 */
	public int calculatePriorityScore(Long userId) {
		return calculatePriorityScore(userId, LocalDateTime.now());
	}

	int calculatePriorityScore(Long userId, LocalDateTime now) {
		boolean prioritized = findLatestByType(userId).stream()
				.anyMatch(latest -> latest.getVerificationType().isPriority()
						&& latest.statusAt(now) == VerificationStatus.APPROVED);
		return prioritized ? 1 : 0;
	}

	// 유효한 토큰인데 회원이 없는 경우 — 회원 탈퇴 기능이 없어서 운영진이 DB에서 직접 지운 경우뿐
	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_USER_NOT_FOUND));
	}

}
