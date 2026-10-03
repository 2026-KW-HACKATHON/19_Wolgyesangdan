package com.Wolgyesangdan.backend.domain.item.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 물품 카테고리 대분류. 둘러보기 필터 칩과 탄소 참조표(CategoryCarbonReference)의 기준.
 *
 * 코드 안에서는 enum으로 다루지만, 바깥으로 나가는 값은 전부 한글 표시값(label)이다 —
 * JSON 요청/응답은 @JsonValue, DB 저장값은 CategoryGroupConverter가 label로 변환한다.
 */
@Getter
@RequiredArgsConstructor
public enum CategoryGroup {

	FURNITURE("가구"),
	APPLIANCE("가전"),
	KITCHEN("주방"),
	LIVING("생활"),
	ETC("기타");

	@JsonValue
	private final String label;

	/**
	 * 한글 표시값으로 enum을 찾는다. 일치하는 값이 없으면 IllegalArgumentException.
	 */
	public static CategoryGroup fromLabel(String label) {
		return Arrays.stream(values())
				.filter(group -> group.label.equals(label))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 대분류입니다: " + label));
	}

}
