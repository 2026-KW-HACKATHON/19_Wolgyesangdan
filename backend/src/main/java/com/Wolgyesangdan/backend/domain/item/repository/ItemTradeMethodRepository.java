package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.Collection;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemTradeMethodRepository extends JpaRepository<ItemTradeMethod, Long> {

	List<ItemTradeMethod> findByItemIdIn(Collection<Long> itemIds);

	List<ItemTradeMethod> findByItemId(Long itemId);

	/**
	 * 물품 수정 때 다시 저장하기 전에 지운다. 바로 delete 쿼리를 보낸다 — 엔티티를 지우면 Hibernate가 flush 때
	 * insert를 delete보다 먼저 보내서 같은 값을 다시 넣을 때 유니크 제약에 걸린다
	 */
	@Modifying(flushAutomatically = true)
	@Query("delete from ItemTradeMethod e where e.item.id = :itemId")
	void deleteByItemId(@Param("itemId") Long itemId);

}
