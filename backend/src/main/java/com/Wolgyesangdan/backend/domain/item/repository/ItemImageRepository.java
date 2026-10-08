package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.Collection;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

	List<ItemImage> findByItemIdIn(Collection<Long> itemIds);

	List<ItemImage> findByItemIdOrderByDisplayOrderAsc(Long itemId);

	/** 물품 수정 때 사진을 새 목록으로 다시 저장하기 전에 지운다 (거래 방식과 같은 방식) */
	@Modifying(flushAutomatically = true)
	@Query("delete from ItemImage e where e.item.id = :itemId")
	void deleteByItemId(@Param("itemId") Long itemId);

}
