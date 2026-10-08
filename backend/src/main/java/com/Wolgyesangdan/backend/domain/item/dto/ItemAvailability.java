package com.Wolgyesangdan.backend.domain.item.dto;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;

/**
 * 물품 목록에 보여줄 상태 범위 (#190). 홈은 신청 가능한 물건만, 둘러보기는 거래가 끝난 물건을 기본으로 숨긴다.
 * 취소·등록 대기 물품은 어느 범위에도 나오지 않는다.
 */
public enum ItemAvailability {
	/** 신청 가능 — 홈 "지금 새로운 주인을 기다려요" */
	OPEN(List.of(ItemStatus.OPEN)),
	/** 기본 — 신청 가능 + 신청 마감(배정 대기) */
	ACTIVE(List.of(ItemStatus.OPEN, ItemStatus.CLOSED)),
	/** 거래가 끝난 물건(배정·거래 완료)까지 전부 */
	ALL(List.of(ItemStatus.OPEN, ItemStatus.CLOSED, ItemStatus.ASSIGNED, ItemStatus.COMPLETED));

	private final List<ItemStatus> statuses;

	ItemAvailability(List<ItemStatus> statuses) {
		this.statuses = statuses;
	}

	public List<ItemStatus> statuses() {
		return statuses;
	}
}
