package com.Wolgyesangdan.backend.domain.verification.repository;

import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.entity.VerificationAuditLog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationAuditLogRepository extends JpaRepository<VerificationAuditLog, Long> {

	List<VerificationAuditLog> findByVerificationIdOrderByIdAsc(Long verificationId);

}
