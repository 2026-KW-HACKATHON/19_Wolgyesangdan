package com.Wolgyesangdan.backend.domain.reservation.dto;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;

/** 카테고리 대분류별 거래 완료 탄소 절감량 합계 (집계 쿼리 결과) */
public record CategoryCarbonSum(CategoryGroup categoryGroup, long carbonReductionKg) {
}
