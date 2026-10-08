package com.Wolgyesangdan.backend.domain.verification.scheduler;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.verification.service.VerificationDocumentCleanupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 검토 후 30일 지난 우선배정 서류 삭제. 한 건이 실패해도 나머지는 계속하고, 실패한 건은 다음 실행에서 다시 시도한다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationDocumentCleanupScheduler {

	private final VerificationDocumentCleanupService verificationDocumentCleanupService;

	@Scheduled(fixedDelayString = "${scheduler.document-cleanup.fixed-delay:PT1H}",
			initialDelayString = "${scheduler.document-cleanup.initial-delay:PT1M}")
	public void purgeReviewedDocuments() {
		for (Long verificationId : verificationDocumentCleanupService.findIdsToPurge(LocalDateTime.now())) {
			try {
				verificationDocumentCleanupService.purge(verificationId);
				log.info("인증 신청 {} 서류 삭제 (검토 후 보관 기간 지남)", verificationId);
			} catch (RuntimeException e) {
				log.error("인증 신청 {} 서류 삭제 실패 — 다음 실행에서 다시 시도", verificationId, e);
			}
		}
	}

}
