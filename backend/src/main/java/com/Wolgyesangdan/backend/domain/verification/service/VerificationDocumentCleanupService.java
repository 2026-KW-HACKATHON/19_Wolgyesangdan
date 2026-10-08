package com.Wolgyesangdan.backend.domain.verification.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.repository.PriorityVerificationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검토(승인·반려)가 끝나고 30일이 지난 우선배정 서류를 지운다 — S3 파일과 서류에 적힌 이름.
 * 심사 결과(상태·만료일·반려 사유)와 감사 로그는 남긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationDocumentCleanupService {

	static final Duration RETENTION = Duration.ofDays(30);

	private final PriorityVerificationRepository priorityVerificationRepository;
	private final VerificationDocumentStorage verificationDocumentStorage;

	/** 지울 서류가 있는 신청 id. S3 설정이 없으면(로컬) 지울 수 없으니 빈 목록 */
	public List<Long> findIdsToPurge(LocalDateTime now) {
		if (!verificationDocumentStorage.isAvailable()) {
			return List.of();
		}
		return priorityVerificationRepository.findIdsWithDocumentReviewedBefore(now.minus(RETENTION));
	}

	/**
	 * S3 파일을 먼저 지우고 DB의 서류 정보를 비운다. S3 삭제가 실패하면 DB를 그대로 둬서 다음 실행에서 다시 시도한다
	 * (S3 삭제는 같은 파일을 여러 번 해도 괜찮다).
	 */
	@Transactional
	public void purge(Long verificationId) {
		priorityVerificationRepository.findById(verificationId)
				.filter(verification -> verification.getFileKey() != null)
				.ifPresent(this::purge);
	}

	private void purge(PriorityVerification verification) {
		verificationDocumentStorage.delete(verification.getFileKey());
		verification.purgeDocument();
	}

}
