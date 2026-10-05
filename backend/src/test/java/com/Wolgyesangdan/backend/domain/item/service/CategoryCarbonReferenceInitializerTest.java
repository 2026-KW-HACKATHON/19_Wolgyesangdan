package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 동작 확인. 각 테스트는 참조표를 비운 상태에서 시작하고, 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, CategoryCarbonReferenceInitializer.class})
class CategoryCarbonReferenceInitializerTest {

	@Autowired
	private CategoryCarbonReferenceInitializer initializer;

	@Autowired
	private CategoryCarbonReferenceRepository repository;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void clear() {
		entityManager.createQuery("delete from CategoryCarbonReference").executeUpdate();
	}

	@Test
	void 빈_테이블에_5개_대분류를_채운다() {
		run();

		assertThat(repository.findAll())
				.extracting(CategoryCarbonReference::getCategoryGroup, CategoryCarbonReference::getCarbonReductionKg)
				.containsExactlyInAnyOrder(
						tuple(CategoryGroup.FURNITURE, 35),
						tuple(CategoryGroup.APPLIANCE, 70),
						tuple(CategoryGroup.KITCHEN, 8),
						tuple(CategoryGroup.LIVING, 15),
						tuple(CategoryGroup.ETC, 8));
	}

	@Test
	void 여러_번_실행해도_중복으로_쌓이지_않는다() {
		run();
		run();

		assertThat(repository.count()).isEqualTo(CategoryGroup.values().length);
	}

	@Test
	void DB_값이_코드와_다르면_코드_값으로_갱신한다() {
		repository.save(CategoryCarbonReference.builder()
				.categoryGroup(CategoryGroup.FURNITURE)
				.carbonReductionKg(999)
				.source("예전 값")
				.build());

		run();

		CategoryCarbonReference furniture = repository.findByCategoryGroup(CategoryGroup.FURNITURE).orElseThrow();
		assertThat(furniture.getCarbonReductionKg()).isEqualTo(35);
		assertThat(furniture.getSource()).startsWith("WRAP(2012)");
	}

	@Test
	void 모든_대분류에_참조값이_정의돼_있다() {
		assertThat(CategoryCarbonReferenceInitializer.REFERENCES.keySet())
				.containsExactlyInAnyOrder(CategoryGroup.values());
	}

	private void run() {
		initializer.run(new DefaultApplicationArguments());
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void 모든_참조값에_출처가_있고_컬럼_길이_안이다() {
		assertThat(CategoryCarbonReferenceInitializer.REFERENCES.values())
				.allSatisfy(reference -> {
					assertThat(reference.carbonReductionKg()).isPositive();
					assertThat(reference.source()).isNotBlank().hasSizeLessThanOrEqualTo(255);
				});
	}

	@Test
	void 값은_같아도_출처가_바뀌면_갱신한다() {
		repository.save(CategoryCarbonReference.builder()
				.categoryGroup(CategoryGroup.LIVING)
				.carbonReductionKg(15)
				.source("임시값(placeholder)")
				.build());

		run();

		assertThat(repository.findByCategoryGroup(CategoryGroup.LIVING).orElseThrow().getSource())
				.startsWith("UK DESNZ 2024");
	}

}
