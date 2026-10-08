package com.Wolgyesangdan.backend.domain.inquiry.entity;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.entity.BaseTimeEntity;

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

/**
 * 회원이 운영자에게 남기는 문의. 답변은 한 번만 달 수 있다 (수정·삭제 없음).
 */
@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Inquiry extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private InquiryCategory category;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	@Builder.Default
	private InquiryStatus status = InquiryStatus.OPEN;

	@Column(columnDefinition = "TEXT")
	private String answer;

	private LocalDateTime answeredAt;

	/** 답변한 운영자 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "answered_by")
	private User answeredBy;

	public boolean isAnswered() {
		return status == InquiryStatus.ANSWERED;
	}

	/** 운영자 답변 — 답변 완료로 바꾸고 누가·언제 답했는지 기록한다 */
	public void answer(String answer, User admin, LocalDateTime now) {
		this.answer = answer;
		this.answeredBy = admin;
		this.answeredAt = now;
		this.status = InquiryStatus.ANSWERED;
	}
}
