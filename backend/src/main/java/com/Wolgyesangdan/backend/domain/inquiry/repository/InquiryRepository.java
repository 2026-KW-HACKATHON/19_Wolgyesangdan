package com.Wolgyesangdan.backend.domain.inquiry.repository;

import java.util.Optional;

import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	/** 내 문의 — 정렬은 Pageable로 준다 */
	Page<Inquiry> findByAuthorId(Long authorId, Pageable pageable);

	/**
	 * 관리자 문의 목록 — 답변 대기(OPEN)가 먼저, 그 안에서는 최근에 들어온 순. status가 null이면 전체.
	 * 정렬을 쿼리에 적었으므로 Pageable에는 정렬을 넣지 않는다.
	 */
	@EntityGraph(attributePaths = "author")
	@Query(value = """
			select i from Inquiry i
			where :status is null or i.status = :status
			order by case when i.status = com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus.OPEN then 0 else 1 end,
				i.createdAt desc, i.id desc
			""",
			countQuery = "select count(i) from Inquiry i where :status is null or i.status = :status")
	Page<Inquiry> findAllForAdmin(@Param("status") InquiryStatus status, Pageable pageable);

	/** 관리자 상세용 — 작성자를 한 번에 가져온다 */
	@EntityGraph(attributePaths = "author")
	Optional<Inquiry> findWithAuthorById(Long id);

	/** 대시보드 요약용 (#216) */
	long countByStatus(InquiryStatus status);

}
