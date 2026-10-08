package com.Wolgyesangdan.backend.domain.item.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Item extends BaseTimeEntity {

	/** 물품당 신청 정원 (고정값) */
	public static final int MAX_APPLICANTS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "campaign_id")
	private Campaign campaign;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 20)
	@Convert(converter = CategoryGroupConverter.class)
	private CategoryGroup categoryGroup;

	@Column(length = 30)
	private String category;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, length = 20)
	private String conditionGrade;

	@Column(length = 50)
	private String usagePeriod;

	@Column(nullable = false)
	private boolean defectYn;

	@Column(columnDefinition = "TEXT")
	private String defectDescription;

	@Column(length = 20)
	private String workingStatus;

	@Column(length = 50)
	private String size;

	@Column(length = 20)
	private String transportDifficulty;

	@Column(nullable = false)
	private int estimatedCarbonReduction;

	private LocalDate availableFrom;

	private LocalDate availableUntil;

	private LocalDate disposalDeadline;

	@Column(nullable = false)
	private LocalDateTime applicationDeadline;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private ItemStatus status;

	@Column(nullable = false)
	private int applicantCount;

	// 관리자가 숨긴 물품 — 회원 앱 목록·상세에서 빠진다 (#214).
	// 운영 DB가 ddl-auto: update라 컬럼이 추가될 때 기존 물품 행에도 false가 들어가도록 DB 기본값을 둔다
	@ColumnDefault("false")
	@Column(nullable = false)
	private boolean hidden;

	/**
	 * 아직 배정 전인지 — 신청을 받는 중이거나, 마감됐지만 배정을 기다리는 중.
	 * 이때는 신청자에게 대기 순번을 알려주지 않는다 (#265) — 우선배정 인증으로 순번이 앞서는 것이
	 * 신청 도중에 드러나지 않도록, 순번은 배정이 끝난 뒤에만 공개한다.
	 */
	public boolean isBeforeAssignment() {
		return status == ItemStatus.REGISTERED || status == ItemStatus.OPEN || status == ItemStatus.CLOSED;
	}

	/** 신청 접수 — 신청자 수를 늘리고, 정원(MAX_APPLICANTS)에 도달하면 더 이상 받지 않도록 CLOSED로 전환한다. */
	public void increaseApplicantCount() {
		this.applicantCount++;
		if (this.applicantCount >= MAX_APPLICANTS) {
			this.status = ItemStatus.CLOSED;
		}
	}

	/**
	 * 신청 취소 — 신청자 수를 줄이고, 정원이 꽉 차서 CLOSED였다면 신청 마감 전인 경우에만 다시 OPEN으로 되돌린다
	 * (요구사항 명세서 미정 사항 4번, 2026-10-05 결정).
	 */
	public void decreaseApplicantCount(LocalDateTime now) {
		this.applicantCount--;
		if (this.status == ItemStatus.CLOSED && this.applicationDeadline.isAfter(now)) {
			this.status = ItemStatus.OPEN;
		}
	}

	/** 배정 확정 — 전달·수령이 끝날 때까지 "예약 중" */
	public void assign() {
		this.status = ItemStatus.ASSIGNED;
	}

	/** 받을 사람이 없어 종료 (2026-10-05 결정 — 신청자 0명이거나 승계할 대기자가 없을 때) */
	public void cancel() {
		this.status = ItemStatus.CANCELED;
	}

	/** 거래 완료 — 탄소 절감량 집계에 들어간다 */
	public void complete() {
		this.status = ItemStatus.COMPLETED;
	}

	/** 관리자 숨기기 — 회원 앱 목록·상세에서 빠진다. 상태(status)는 건드리지 않는다 */
	public void hide() {
		this.hidden = true;
	}

	/** 숨긴 물품 다시 보이기 */
	public void show() {
		this.hidden = false;
	}
}
