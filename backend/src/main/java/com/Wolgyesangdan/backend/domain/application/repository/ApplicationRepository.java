package com.Wolgyesangdan.backend.domain.application.repository;

import java.util.List;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.item.entity.Item;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

	boolean existsByItemIdAndApplicantId(Long itemId, Long applicantId);

	/**
	 * 대기 순번 재계산용 — priority_score 높은 순, 같으면 먼저 신청한(appliedAt 빠른) 순.
	 * createdAt이 같을 수 있어 id를 마지막 기준으로 둬서 순서를 고정한다.
	 */
	List<Application> findByItemAndStatusOrderByPriorityScoreDescCreatedAtAscIdAsc(Item item, ApplicationStatus status);

}
