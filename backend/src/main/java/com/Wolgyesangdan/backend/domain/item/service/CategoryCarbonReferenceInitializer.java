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

	// ⚠️ 임시값(placeholder) — 실측 EPD/LCA 데이터로 교체 필요 (#16)
	static final String SOURCE = "임시값(placeholder) — 실측 EPD/LCA 데이터로 교체 필요";
	static final Map<CategoryGroup, Integer> CARBON_REDUCTION_KG = new EnumMap<>(Map.of(
			CategoryGroup.FURNITURE, 30,
			CategoryGroup.APPLIANCE, 24,
			CategoryGroup.KITCHEN, 10,
			CategoryGroup.LIVING, 15,
			CategoryGroup.ETC, 8));

	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (CategoryGroup categoryGroup : CategoryGroup.values()) {
			Integer carbonReductionKg = CARBON_REDUCTION_KG.get(categoryGroup);
			if (carbonReductionKg == null) {
				throw new IllegalStateException("탄소 참조값이 정의되지 않은 카테고리 대분류: " + categoryGroup
						+ " — CategoryCarbonReferenceInitializer.CARBON_REDUCTION_KG에 추가하세요.");
			}
			categoryCarbonReferenceRepository.findByCategoryGroup(categoryGroup).ifPresentOrElse(
					reference -> updateIfChanged(reference, carbonReductionKg),
					() -> categoryCarbonReferenceRepository.save(CategoryCarbonReference.builder()
							.categoryGroup(categoryGroup)
							.carbonReductionKg(carbonReductionKg)
							.source(SOURCE)
							.build()));
		}
		log.info("탄소 참조표 동기화 완료: {}", Arrays.toString(CategoryGroup.values()));
	}

	// 값이 같으면 건드리지 않는다 — updated_at이 "실제로 값이 바뀐 시각"을 가리키게 하기 위함
	private void updateIfChanged(CategoryCarbonReference reference, int carbonReductionKg) {
		if (reference.getCarbonReductionKg() != carbonReductionKg || !Objects.equals(reference.getSource(), SOURCE)) {
			reference.update(carbonReductionKg, SOURCE);
		}
	}

}
