package com.Wolgyesangdan.backend.domain.campaign.entity;

import java.time.LocalDate;

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
	@Column(nullable = false, length = 20)
	private CampaignStatus status;
}
