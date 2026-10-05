package com.Wolgyesangdan.backend.domain.application.repository;

import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.entity.Item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

	boolean existsByItemIdAndApplicantId(Long itemId, Long applicantId);

	/** 마이페이지 "내가 신청한 물품" — 물품을 같이 가져와서 itemName 조회 시 N+1이 안 생기게 한다 */
	@EntityGraph(attributePaths = {"item"})
	Page<Application> findByApplicantId(Long applicantId, Pageable pageable);

	/**
	 * 대기 순번 재계산용 — priority_score 높은 순, 같으면 먼저 신청한(appliedAt 빠른) 순.
	 * createdAt이 같을 수 있어 id를 마지막 기준으로 둬서 순서를 고정한다.
	 */
	List<Application> findByItemAndStatusOrderByPriorityScoreDescCreatedAtAscIdAsc(Item item, ApplicationStatus status);

	/** 취소용 — 물품을 잠그기 전에 물품 id만 가볍게 조회한다 (Application을 먼저 영속성 컨텍스트에 올리지 않기 위함) */
	@Query("select a.item.id from Application a where a.id = :id")
	Optional<Long> findItemIdById(@Param("id") Long id);

}
