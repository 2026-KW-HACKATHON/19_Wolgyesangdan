package com.Wolgyesangdan.backend.domain.item.dto;

import java.time.LocalDate;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 물품 등록 요청. 필수는 사진·물품명·카테고리 대분류·상태 등급·거래 방식 5가지 (2026-10-05 결정, #56).
 * 예상 탄소 절감량·신청 마감 시각·상태·신청자 수는 받지 않고 서버가 정한다.
 */
public record ItemCreateRequest(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 30) String category,
		@NotNull CategoryGroup categoryGroup,
		@Size(max = 2000) String description,
		@NotBlank
		@Pattern(regexp = "^(거의 새것|상태 좋음|사용감 있음)$",
				message = "상태 등급은 거의 새것, 상태 좋음, 사용감 있음 중 하나여야 합니다.")
		String conditionGrade,
		@Size(max = 50) String usagePeriod,
		Boolean defectYn,
		@Size(max = 1000) String defectDescription,
		@Size(max = 20) String workingStatus,
		@Size(max = 50) String size,
		@Pattern(regexp = "^(쉬움|보통|어려움)$", message = "운반 난이도는 쉬움, 보통, 어려움 중 하나여야 합니다.")
		String transportDifficulty,
		LocalDate availableFrom,
		LocalDate availableUntil,
		LocalDate disposalDeadline,
		@NotEmpty List<@NotNull TradeMethod> tradeMethods,
		Long campaignId,
		@NotEmpty
		@Size(max = 5, message = "사진은 최대 5장까지 올릴 수 있습니다.")
		List<@NotBlank @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "올바른 이미지 주소가 아닙니다.") String> imageUrls) {

	@AssertTrue(message = "전달 가능 시작일은 종료일보다 늦을 수 없습니다.")
	public boolean isAvailablePeriodValid() {
		return availableFrom == null || availableUntil == null || !availableFrom.isAfter(availableUntil);
	}

}
