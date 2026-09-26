package com.Wolgyesangdan.backend.domain.verification.entity;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "priority_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PriorityVerification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationType verificationType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationStatus status;

	@Column(length = 30)
	private String studentId;

	@Column(length = 50)
	private String department;

	private Integer admissionYear;

	@Column(length = 50)
	private String name;

	@Column(length = 255)
	private String address;

	@Builder.Default
	@Column(nullable = false)
	private LocalDateTime submittedAt = LocalDateTime.now();

	private LocalDateTime reviewedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by")
	private User reviewedBy;

	@Column(length = 255)
	private String rejectionReason;

	private LocalDateTime expiresAt;
}
