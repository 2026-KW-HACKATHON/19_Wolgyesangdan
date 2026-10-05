package com.Wolgyesangdan.backend.domain.verification.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import com.Wolgyesangdan.backend.global.exception.CommonErrorCode;

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

	// 프론트 StudentVerificationFormPage의 검증과 동일 (숫자 4~12자). 앞 4자리가 입학연도
	private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{4,12}$");
	private static final int FIRST_ADMISSION_YEAR = 1934; // 광운대 개교 연도

	private final PriorityVerificationRepository priorityVerificationRepository;
	private final UserRepository userRepository;

	/**
	 * 이웃 인증 신청. 항상 PENDING으로 접수하고, 실제 확인·승인은 운영진이 앱 밖에서 한다.
	 * 같은 유형에 심사 중인 신청이나 유효한 승인이 있으면 거절한다 (반려·만료된 뒤에는 다시 신청 가능).
	 */
	@Transactional
	public VerificationCreateResponse createVerification(Long userId, VerificationCreateRequest request) {
		return createVerification(userId, request, LocalDateTime.now());
	}

	VerificationCreateResponse createVerification(Long userId, VerificationCreateRequest request, LocalDateTime now) {
		VerificationType type = request.verificationType();
		PriorityVerification.PriorityVerificationBuilder verification = PriorityVerification.builder()
				.verificationType(type)
				.status(VerificationStatus.PENDING)
				.submittedAt(now);
		// 해당 유형의 값만 저장하고 나머지는 보내도 무시한다
		switch (type) {
			case STUDENT -> {
				String studentId = request.studentId().strip();
				verification.studentId(studentId)
						.department(request.department().strip())
						.admissionYear(parseAdmissionYear(studentId, now.getYear()));
			}
			case RESIDENT -> verification.name(request.name().strip()).address(request.address().strip());
			case LOW_INCOME -> {
			}
		}
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
		return VerificationCreateResponse.from(
				priorityVerificationRepository.save(verification.user(findUser(userId)).build()));
	}

	/**
	 * 내 인증 상태. 한 번이라도 신청한 유형만, 유형마다 가장 최근 제출 건 기준으로 내려준다.
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
	 * 유형별 가장 최근 제출 건 (RESIDENT → STUDENT → LOW_INCOME 순). 물품 신청 자격 체크도 이 조회를 쓴다.
	 * 승인 여부는 getStatus()가 아니라 statusAt(now)로 판단해야 만료된 인증이 통과하지 않는다.
	 */
	public Collection<PriorityVerification> findLatestByType(Long userId) {
		// 한 사용자의 인증 신청은 유형별로 몇 건뿐이라 전부 읽어서 고른다
		return priorityVerificationRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(PriorityVerification::getVerificationType, verification -> verification,
						BinaryOperator.maxBy(BY_RECENCY), () -> new EnumMap<>(VerificationType.class)))
				.values();
	}

	// 입학연도는 따로 받지 않고 학번 앞 4자리에서 계산한다. 개교 전이거나 올해보다 뒤면 잘못된 학번
	private int parseAdmissionYear(String studentId, int thisYear) {
		if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_INVALID_STUDENT_ID);
		}
		int admissionYear = Integer.parseInt(studentId.substring(0, 4));
		if (admissionYear < FIRST_ADMISSION_YEAR || admissionYear > thisYear) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_INVALID_STUDENT_ID);
		}
		return admissionYear;
	}

	// 유효한 토큰인데 회원이 없는 경우 — 회원 탈퇴 기능이 없어서 운영진이 DB에서 직접 지운 경우뿐
	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
	}

}
