package com.Wolgyesangdan.backend.domain.campaign.entity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Stream;

import com.Wolgyesangdan.backend.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "campaigns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Campaign extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private LocalDate registrationStartDate;

	@Column(nullable = false)
	private LocalDate registrationEndDate;

	@Column(nullable = false)
	private LocalDate applicationStartDate;

	@Column(nullable = false)
	private LocalDate applicationEndDate;

	@Column(nullable = false)
	private LocalDate pickupStartDate;

	@Column(nullable = false)
	private LocalDate pickupEndDate;

	@Column(nullable = false, length = 100)
	private String locationName;

	@Column(nullable = false, length = 255)
	private String locationAddress;

	@Column(length = 100)
	private String hubHours;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CampaignStatus status;

	/** 캠페인 전체 기간의 시작일 — 등록/신청/수령 기간 중 가장 이른 시작일 */
	public LocalDate periodStart() {
		return Stream.of(registrationStartDate, applicationStartDate, pickupStartDate)
				.min(Comparator.naturalOrder())
				.orElseThrow();
	}

	/** 캠페인 전체 기간의 종료일 — 등록/신청/수령 기간 중 가장 늦은 종료일 */
	public LocalDate periodEnd() {
		return Stream.of(registrationEndDate, applicationEndDate, pickupEndDate)
				.max(Comparator.naturalOrder())
				.orElseThrow();
	}

	/**
	 * 날짜 기준 진행 상태. DB의 status 컬럼은 운영진이 바꾸는 걸 잊을 수 있어서 쓰지 않는다 (#43).
	 * 시작일·종료일 당일은 진행 중으로 본다.
	 */
	public CampaignStatus statusOn(LocalDate date) {
		if (date.isBefore(periodStart())) {
			return CampaignStatus.PLANNED;
		}
		if (date.isAfter(periodEnd())) {
			return CampaignStatus.ENDED;
		}
		return CampaignStatus.ACTIVE;
	}
}
