package com.Wolgyesangdan.backend.domain.item.service;

import java.util.Comparator;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository;

	/**
	 * 카테고리 대분류별 예상 탄소 절감량. 순서는 CategoryGroup 선언 순서(가구·가전·주방·생활·기타)로 고정 —
	 * 프론트 필터 칩/등록 폼이 이 순서 그대로 그린다.
	 */
	public List<CategoryResponse> getCategories() {
		return categoryCarbonReferenceRepository.findAll().stream()
				.sorted(Comparator.comparing(CategoryCarbonReference::getCategoryGroup))
				.map(CategoryResponse::from)
				.toList();
	}

}
