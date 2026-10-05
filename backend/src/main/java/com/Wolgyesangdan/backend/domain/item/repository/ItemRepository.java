package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.Collection;

import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

	@Query("""
			select new com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary(
				count(i), coalesce(sum(i.estimatedCarbonReduction), 0L))
			from Item i
			where i.campaign.id = :campaignId
				and i.status = com.Wolgyesangdan.backend.domain.item.entity.ItemStatus.COMPLETED
			""")
	CompletedItemSummary summarizeCompletedByCampaignId(@Param("campaignId") Long campaignId);

	Page<Item> findByStatusIn(Collection<ItemStatus> statuses, Pageable pageable);

}
