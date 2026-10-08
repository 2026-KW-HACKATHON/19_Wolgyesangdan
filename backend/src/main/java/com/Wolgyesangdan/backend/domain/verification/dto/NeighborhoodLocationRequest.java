package com.Wolgyesangdan.backend.domain.verification.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 브라우저가 측위한 현재 위치. 판정에만 쓰고 저장하지 않는다.
 *
 * @param accuracy 오차 반경 (m)
 */
public record NeighborhoodLocationRequest(
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
		@NotNull @PositiveOrZero Double accuracy) {
}
