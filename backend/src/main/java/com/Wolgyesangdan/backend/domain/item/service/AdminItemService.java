package com.Wolgyesangdan.backend.domain.item.service;

import com.Wolgyesangdan.backend.domain.item.dto.AdminItemResponse;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.item.repository.ItemRepository;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 물품 관리 (#214). 숨기기만 있고 영구 삭제는 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminItemService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	private final ItemRepository itemRepository;

	/**
	 * 물품 목록 — 상태와 관계없이 전부, 최근 등록순 (요청의 sort는 쓰지 않는다). 한 페이지는 최대 100개.
	 *
	 * @param hidden true면 숨긴 물품만, false면 보이는 물품만, null이면 전부
	 */
	public Page<AdminItemResponse> getItems(Boolean hidden, Pageable pageable) {
		return itemRepository.findAllForAdmin(hidden,
						PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), LATEST))
				.map(AdminItemResponse::from);
	}

	/**
	 * 숨기기 / 다시 보이기. 물품 상태·신청·예약은 그대로 두고 회원 화면 노출만 바꾼다.
	 */
	@Transactional
	public AdminItemResponse changeHidden(Long itemId, boolean hidden) {
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
		if (hidden) {
			item.hide();
		} else {
			item.show();
		}
		return AdminItemResponse.from(item);
	}

}
