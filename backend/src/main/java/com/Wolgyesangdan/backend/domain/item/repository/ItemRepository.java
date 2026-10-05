package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.Optional;

import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
import com.Wolgyesangdan.backend.domain.item.entity.Item;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

	@Query("""
			select new com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary(
				count(i), coalesce(sum(i.estimatedCarbonReduction), 0L))
			from Item i
			where i.campaign.id = :campaignId
				and i.status = com.Wolgyesangdan.backend.domain.item.entity.ItemStatus.COMPLETED
			""")
	CompletedItemSummary summarizeCompletedByCampaignId(@Param("campaignId") Long campaignId);

	/** 상세 조회용 — 등록자·캠페인을 한 번에 가져온다 */
	@EntityGraph(attributePaths = {"owner", "campaign"})
	Optional<Item> findWithOwnerAndCampaignById(Long id);

}
