package com.Wolgyesangdan.backend.domain.item.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버 기동 시 탄소 참조표(category_carbon_references)를 아래 코드 값과 맞춘다 — 없으면 추가, 다르면 갱신.
 *
 * 값의 기준은 이 코드다. 값을 바꾸려면 여기 숫자를 고쳐서 PR을 올리면 재시작 시 모든 환경에 반영된다.
 * 물품에는 등록 시점 값이 스냅샷으로 저장되므로, 값을 바꿔도 이미 등록된 물품의 값은 안 바뀐다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryCarbonReferenceInitializer implements ApplicationRunner {

	/** 카테고리 대분류 하나의 참조값 — 예상 탄소 절감량(kg CO2e)과 그 근거 */
	record Reference(int carbonReductionKg, String source) {
	}

	/**
	 * 재사용 1건이 새 제품 1개의 생산을 대체한다고 보고(1:1 대체, 잠재적 절감량) 그 생산 배출량을 절감량으로 쓴다 (#16, 2026-10-05).
	 * 가구는 WRAP 품목별 연구, 나머지는 영국 정부 환산계수(DESNZ 2024, 신제품 생산 kg CO2e/t)에 대표 물품 무게를 곱했다.
	 * 동네 안 직거래라 운송 배출은 무시한다. 자세한 근거는 요구사항 명세서 6.5 "탄소 절감량" 참고.
	 */
	static final Map<CategoryGroup, Reference> REFERENCES = new EnumMap<>(Map.of(
			CategoryGroup.FURNITURE, new Reference(35,
					"WRAP(2012) 가구 재사용 연구: 소파 40~55kg, 식탁 20kg의 중간값"),
			CategoryGroup.APPLIANCE, new Reference(70,
					"UK DESNZ 2024 소형 가전 생산 5,648kgCO2e/t × 전자레인지 12kg"),
			CategoryGroup.KITCHEN, new Reference(8,
					"UK DESNZ 2024 금속 생산 3,465kgCO2e/t × 냄비·프라이팬 2kg"),
			CategoryGroup.LIVING, new Reference(15,
					"UK DESNZ 2024 플라스틱 평균 생산 3,165kgCO2e/t × 플라스틱 수납장 5kg"),
			CategoryGroup.ETC, new Reference(8,
					"UK DESNZ 2024 플라스틱 평균 생산 3,165kgCO2e/t × 플라스틱 소품 2.5kg")));

	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (CategoryGroup categoryGroup : CategoryGroup.values()) {
			Reference expected = REFERENCES.get(categoryGroup);
			if (expected == null) {
				throw new IllegalStateException("탄소 참조값이 정의되지 않은 카테고리 대분류: " + categoryGroup
						+ " — CategoryCarbonReferenceInitializer.REFERENCES에 추가하세요.");
			}
			categoryCarbonReferenceRepository.findByCategoryGroup(categoryGroup).ifPresentOrElse(
					reference -> updateIfChanged(reference, expected),
					() -> categoryCarbonReferenceRepository.save(CategoryCarbonReference.builder()
							.categoryGroup(categoryGroup)
							.carbonReductionKg(expected.carbonReductionKg())
							.source(expected.source())
							.build()));
		}
		log.info("탄소 참조표 동기화 완료: {}", Arrays.toString(CategoryGroup.values()));
	}

	// 값이 같으면 건드리지 않는다 — updated_at이 "실제로 값이 바뀐 시각"을 가리키게 하기 위함
	private void updateIfChanged(CategoryCarbonReference reference, Reference expected) {
		if (reference.getCarbonReductionKg() != expected.carbonReductionKg()
				|| !Objects.equals(reference.getSource(), expected.source())) {
			reference.update(expected.carbonReductionKg(), expected.source());
		}
	}

}
