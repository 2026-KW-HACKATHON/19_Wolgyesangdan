package com.Wolgyesangdan.backend.domain.verification.repository;

import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PriorityVerificationRepository extends JpaRepository<PriorityVerification, Long> {

	List<PriorityVerification> findByUserId(Long userId);

}
