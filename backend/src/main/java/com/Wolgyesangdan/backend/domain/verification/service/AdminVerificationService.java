package com.Wolgyesangdan.backend.domain.verification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationApproveRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationDetailResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationFileResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationSummaryResponse;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationAuditAction;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationAuditLog;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;
import com.Wolgyesangdan.backend.domain.verification.repository.VerificationAuditLogRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 우선배정 서류 심사 (신입생·기초수급자). 동네 인증은 심사가 없어서 다루지 않는다.
 * 서류 열람·승인·반려는 감사 로그에 남긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVerificationService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Sort LATEST = Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"));
	private static final Set<VerificationType> PRIORITY_TYPES =
			EnumSet.of(VerificationType.FRESHMAN, VerificationType.LOW_INCOME);
	private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

	private final PriorityVerificationRepository priorityVerificationRepository;
	private final VerificationAuditLogRepository verificationAuditLogRepository;
	private final UserRepository userRepository;
	private final VerificationService verificationService;
	private final VerificationDocumentStorage verificationDocumentStorage;

	/**
	 * 서류 목록 — 최근 신청순 (요청의 sort는 쓰지 않는다). 한 페이지는 최대 100개.
	 *
	 * @param status PENDING·APPROVED·REJECTED 중 하나, null이면 전부 (APPROVED에는 만료된 건도 EXPIRED로 섞여 나온다)
	 */
	public Page<AdminVerificationSummaryResponse> getVerifications(VerificationStatus status, Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		return priorityVerificationRepository.findForAdmin(PRIORITY_TYPES, status,
						PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), LATEST))
				.map(verification -> AdminVerificationSummaryResponse.of(verification, now));
	}

	public AdminVerificationDetailResponse getVerification(Long verificationId) {
		return toDetail(findPriorityVerification(verificationId), LocalDateTime.now());
	}

	/** 서류 열람 URL. 열람할 때마다 감사 로그를 남긴다 */
	@Transactional
	public AdminVerificationFileResponse issueFileUrl(Long adminId, Long verificationId) {
		LocalDateTime now = LocalDateTime.now();
		PriorityVerification verification = findPriorityVerification(verificationId);
		if (verification.getFileKey() == null) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_DOCUMENT_NOT_FOUND);
		}
		AdminVerificationFileResponse response = verificationDocumentStorage.issueViewUrl(verification.getFileKey(), now);
		recordAudit(verification, findAdmin(adminId), VerificationAuditAction.VIEW_FILE, now);
		return response;
	}

	/**
	 * 승인. 만료일 — 신입생: 입학한 해 12월 31일 23:59:59 (관리자가 서류를 보고 입학 연도를 고름, 올해 또는 내년),
	 * 기초수급자: 승인 시각 + 1년.
	 */
	@Transactional
	public AdminVerificationDetailResponse approve(Long adminId, Long verificationId,
			AdminVerificationApproveRequest request) {
		return approve(adminId, verificationId, request, LocalDateTime.now());
	}

	AdminVerificationDetailResponse approve(Long adminId, Long verificationId, AdminVerificationApproveRequest request,
			LocalDateTime now) {
		PriorityVerification verification = findPendingVerification(verificationId);
		LocalDateTime expiresAt = switch (verification.getVerificationType()) {
			case FRESHMAN -> freshmanExpiresAt(request == null ? null : request.admissionYear(), now);
			case LOW_INCOME -> now.plusYears(1);
			// findPriorityVerification이 동네 인증을 걸러낸다
			case NEIGHBORHOOD -> throw new IllegalStateException("동네 인증은 심사 대상이 아님");
		};
		User admin = findAdmin(adminId);
		verification.approve(admin, now, expiresAt);
		recordAudit(verification, admin, VerificationAuditAction.APPROVE, now);
		return toDetail(verification, now);
	}

	/** 반려. 사유는 회원 앱 인증 화면에 보이고, 회원은 다시 신청할 수 있다 */
	@Transactional
	public AdminVerificationDetailResponse reject(Long adminId, Long verificationId, String reason) {
		return reject(adminId, verificationId, reason, LocalDateTime.now());
	}

	AdminVerificationDetailResponse reject(Long adminId, Long verificationId, String reason, LocalDateTime now) {
		PriorityVerification verification = findPendingVerification(verificationId);
		User admin = findAdmin(adminId);
		verification.reject(admin, now, reason.strip());
		recordAudit(verification, admin, VerificationAuditAction.REJECT, now);
		return toDetail(verification, now);
	}

	private LocalDateTime freshmanExpiresAt(Integer admissionYear, LocalDateTime now) {
		if (admissionYear == null) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_ADMISSION_YEAR_INVALID,
					"신입생 승인에는 입학 연도가 필요합니다.");
		}
		// 지난해로 승인하면 바로 만료되고, 너무 먼 해는 서류를 잘못 본 것이라 막는다
		if (admissionYear != now.getYear() && admissionYear != now.getYear() + 1) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_ADMISSION_YEAR_INVALID);
		}
		return LocalDate.of(admissionYear, 12, 31).atTime(END_OF_DAY);
	}

	private AdminVerificationDetailResponse toDetail(PriorityVerification verification, LocalDateTime now) {
		// 프록시를 거치는 public 메서드만 쓴다 (패키지 전용 오버로드는 프록시에서 필드가 비어 있다)
		boolean neighborhoodVerified = verificationService.hasNeighborhoodVerification(verification.getUser().getId());
		return AdminVerificationDetailResponse.of(verification, neighborhoodVerified, now);
	}

	private PriorityVerification findPendingVerification(Long verificationId) {
		PriorityVerification verification = findPriorityVerification(verificationId);
		if (verification.getStatus() != VerificationStatus.PENDING) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_ALREADY_REVIEWED);
		}
		return verification;
	}

	// 동네 인증은 관리자 화면에서 다루지 않으므로 없는 신청으로 본다
	private PriorityVerification findPriorityVerification(Long verificationId) {
		return priorityVerificationRepository.findWithUsersById(verificationId)
				.filter(verification -> PRIORITY_TYPES.contains(verification.getVerificationType()))
				.orElseThrow(() -> new BusinessException(VerificationErrorCode.VERIFICATION_NOT_FOUND));
	}

	private User findAdmin(Long adminId) {
		return userRepository.findById(adminId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_USER_NOT_FOUND));
	}

	private void recordAudit(PriorityVerification verification, User admin, VerificationAuditAction action,
			LocalDateTime now) {
		verificationAuditLogRepository.save(VerificationAuditLog.builder()
				.verification(verification)
				.admin(admin)
				.action(action)
				.actedAt(now)
				.build());
	}

}
