package com.Wolgyesangdan.backend.domain.application.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;

/**
 * 신청 결과. waitlistRank는 배정 전에는 알려주지 않으므로 신청 직후에는 항상 null이다 (#265).
 */
public record ApplicationCreateResponse(
		Long id,
		Long itemId,
		ApplicationStatus status,
		Integer waitlistRank,
		LocalDateTime appliedAt) {

	public static ApplicationCreateResponse from(Application application) {
		return new ApplicationCreateResponse(
				application.getId(),
				application.getItem().getId(),
				application.getStatus(),
				application.visibleWaitlistRank(),
				application.getCreatedAt());
	}

}
