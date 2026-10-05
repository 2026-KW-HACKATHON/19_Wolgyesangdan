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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private VerificationType verificationType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
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

	/**
	 * 조회 시점 기준 실제 상태. 승인을 EXPIRED로 바꿔주는 처리가 따로 없어서,
	 * 승인이라도 expiresAt이 지났으면 EXPIRED로 본다. 화면 표시와 신청 자격 체크 모두 이 값을 쓴다.
	 */
	public VerificationStatus statusAt(LocalDateTime now) {
		if (status == VerificationStatus.APPROVED && expiresAt != null && expiresAt.isBefore(now)) {
			return VerificationStatus.EXPIRED;
		}
		return status;
	}
}
