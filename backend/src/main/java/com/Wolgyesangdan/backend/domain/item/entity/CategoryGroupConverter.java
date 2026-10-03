package com.Wolgyesangdan.backend.domain.item.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CategoryGroup을 DB에 enum 이름(FURNITURE)이 아니라 한글 표시값("가구")으로 저장한다.
 * API 명세·기존 데이터와 같은 값을 유지하기 위함.
 */
@Converter
public class CategoryGroupConverter implements AttributeConverter<CategoryGroup, String> {

	@Override
	public String convertToDatabaseColumn(CategoryGroup categoryGroup) {
		return categoryGroup == null ? null : categoryGroup.getLabel();
	}

	@Override
	public CategoryGroup convertToEntityAttribute(String label) {
		return label == null ? null : CategoryGroup.fromLabel(label);
	}

}
