package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.Collection;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

	List<ItemImage> findByItemIdIn(Collection<Long> itemIds);

}
