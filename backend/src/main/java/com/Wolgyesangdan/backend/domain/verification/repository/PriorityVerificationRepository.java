package com.Wolgyesangdan.backend.domain.verification.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriorityVerificationRepository extends JpaRepository<PriorityVerification, Long> {

	List<PriorityVerification> findByUserId(Long userId);

	boolean existsByUserIdAndVerificationTypeAndStatus(Long userId, VerificationType verificationType,
			VerificationStatus status);

	/** 관리자 서류 목록 — 우선배정 유형만. status가 null이면 전부 */
	@EntityGraph(attributePaths = "user")
	@Query("select v from PriorityVerification v where v.verificationType in :types and (:status is null or v.status = :status)")
	Page<PriorityVerification> findForAdmin(@Param("types") Collection<VerificationType> types,
			@Param("status") VerificationStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "reviewedBy"})
	Optional<PriorityVerification> findWithUsersById(Long id);

	/** 대시보드 요약용 — 그 유형들 중 해당 상태인 신청 수 (#216) */
	long countByVerificationTypeInAndStatus(Collection<VerificationType> verificationTypes, VerificationStatus status);

	/** 검토가 끝난 지 보관 기간이 지났는데 서류가 남아 있는 신청 */
	@Query("select v.id from PriorityVerification v where v.fileKey is not null and v.reviewedAt < :cutoff order by v.id")
	List<Long> findIdsWithDocumentReviewedBefore(@Param("cutoff") LocalDateTime cutoff);

}
