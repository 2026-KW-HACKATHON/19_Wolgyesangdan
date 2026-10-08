package com.Wolgyesangdan.backend.domain.campaign.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
	@JdbcTypeCode(SqlTypes.VARCHAR)
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

	/** 캠페인 전체 기간이 시작하는 시각 — 시작일 0시 */
	public LocalDateTime periodStartAt() {
		return periodStart().atStartOfDay();
	}

	/** 캠페인 전체 기간이 끝난 직후 — 종료일 다음 날 0시. 이 시각 "전"까지가 캠페인 기간이다 */
	public LocalDateTime periodEndExclusive() {
		return periodEnd().plusDays(1).atStartOfDay();
	}

	/**
	 * 날짜 기준 진행 상태. DB의 status 컬럼은 운영진이 바꾸는 걸 잊을 수 있어서 예정/진행 중 구분에는 쓰지 않는다 (#43).
	 * 시작일·종료일 당일은 진행 중으로 본다.
	 * 단, 운영진이 관리자 화면에서 운영 중을 끈(status = ENDED) 캠페인은 날짜와 관계없이 끝난 것으로 본다 (#209).
	 */
	public CampaignStatus statusOn(LocalDate date) {
		if (status == CampaignStatus.ENDED) {
			return CampaignStatus.ENDED;
		}
		if (date.isBefore(periodStart())) {
			return CampaignStatus.PLANNED;
		}
		if (date.isAfter(periodEnd())) {
			return CampaignStatus.ENDED;
		}
		return CampaignStatus.ACTIVE;
	}

	/** 관리자 수정 — 바꾸지 않는 값은 지금 값을 그대로 넘긴다 */
	public void update(String name, String description,
			LocalDate registrationStartDate, LocalDate registrationEndDate,
			LocalDate applicationStartDate, LocalDate applicationEndDate,
			LocalDate pickupStartDate, LocalDate pickupEndDate,
			String locationName, String locationAddress, String hubHours) {
		this.name = name;
		this.description = description;
		this.registrationStartDate = registrationStartDate;
		this.registrationEndDate = registrationEndDate;
		this.applicationStartDate = applicationStartDate;
		this.applicationEndDate = applicationEndDate;
		this.pickupStartDate = pickupStartDate;
		this.pickupEndDate = pickupEndDate;
		this.locationName = locationName;
		this.locationAddress = locationAddress;
		this.hubHours = hubHours;
	}

	/** 운영 중 끄기 — 기간이 남아 있어도 끝난 캠페인이 된다 */
	public void end() {
		this.status = CampaignStatus.ENDED;
	}

	/** 운영 중 켜기 — 예정/진행 중은 다시 날짜로 판단한다 */
	public void resume(LocalDate today) {
		this.status = today.isBefore(periodStart()) ? CampaignStatus.PLANNED : CampaignStatus.ACTIVE;
	}
}
