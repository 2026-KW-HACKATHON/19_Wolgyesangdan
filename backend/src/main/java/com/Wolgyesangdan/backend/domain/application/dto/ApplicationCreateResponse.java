package com.Wolgyesangdan.backend.domain.application.dto;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;

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
				application.getWaitlistRank(),
				application.getCreatedAt());
	}

}
