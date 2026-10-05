package com.Wolgyesangdan.backend.global.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 목록 API 공통 페이지 응답. API 명세 "페이지네이션" 규약과 같은 7개 필드만 내려준다.
 *
 * Spring Data의 Page를 그대로 반환하면 pageable/sort 같은 내부 필드가 섞이고, Spring도
 * "구조가 안정적이지 않다"고 경고하므로 이 형식으로 감싸서 반환한다.
 */
public record PageResponse<T>(
		List<T> content,
		long totalElements,
		int totalPages,
		int number,
		int size,
		boolean first,
		boolean last) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
				page.getNumber(), page.getSize(), page.isFirst(), page.isLast());
	}

}
