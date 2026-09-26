package com.Wolgyesangdan.backend.domain.item.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 대분류별 예상 탄소 절감량 참조표.
 * Item과 FK로 연결하지 않는다 — 물품 등록 시점에 조회한 값을 Item.estimatedCarbonReduction에
 * 스냅샷으로 복사해서 저장하고, 이 표의 값이 나중에 바뀌어도 기존 물품 값은 바뀌지 않는다.
 */
@Entity
@Table(name = "category_carbon_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CategoryCarbonReference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, length = 20)
	private String categoryGroup;

	@Column(nullable = false)
	private Integer carbonReductionKg;

	@Column(length = 255)
	private String source;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	@PreUpdate
	void onSave() {
		this.updatedAt = LocalDateTime.now();
	}
}
