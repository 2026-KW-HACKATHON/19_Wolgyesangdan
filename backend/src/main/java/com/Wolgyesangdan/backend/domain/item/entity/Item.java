package com.Wolgyesangdan.backend.domain.item.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
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

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Item extends BaseTimeEntity {

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
	private String categoryGroup;

	@Column(nullable = false, length = 30)
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

	@Column(nullable = false, length = 20)
	private String transportDifficulty;

	@Column(nullable = false)
	private Integer estimatedCarbonReduction;

	@Column(nullable = false)
	private LocalDate availableFrom;

	@Column(nullable = false)
	private LocalDate availableUntil;

	private LocalDate disposalDeadline;

	@Column(nullable = false)
	private LocalDateTime applicationDeadline;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ItemStatus status;

	@Builder.Default
	@Column(nullable = false)
	private Integer applicantCount = 0;
}
