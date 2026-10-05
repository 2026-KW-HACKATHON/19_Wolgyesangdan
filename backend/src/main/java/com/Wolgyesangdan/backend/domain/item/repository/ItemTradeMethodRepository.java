package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.Collection;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemTradeMethodRepository extends JpaRepository<ItemTradeMethod, Long> {

	List<ItemTradeMethod> findByItemIdIn(Collection<Long> itemIds);

	List<ItemTradeMethod> findByItemId(Long itemId);

}
