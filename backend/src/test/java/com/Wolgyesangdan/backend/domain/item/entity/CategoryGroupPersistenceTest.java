package com.Wolgyesangdan.backend.domain.item.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL(로컬 docker / CI 서비스 컨테이너)에 저장했을 때 한글 표시값으로 들어가는지 확인.
 * 테스트 트랜잭션은 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CategoryGroupPersistenceTest {

	@Autowired
	private EntityManager entityManager;

	@Test
	void 한글_표시값으로_저장되고_enum으로_읽힌다() {
		// 참조표에 이미 들어있는 행과 unique 충돌하지 않도록 비우고 시작 (롤백됨)
		entityManager.createQuery("delete from CategoryCarbonReference").executeUpdate();
		CategoryCarbonReference reference = CategoryCarbonReference.builder()
				.categoryGroup(CategoryGroup.FURNITURE)
				.carbonReductionKg(30)
				.build();
		entityManager.persist(reference);
		entityManager.flush();
		entityManager.clear();

		Object stored = entityManager.createNativeQuery(
						"select category_group from category_carbon_references where id = ?")
				.setParameter(1, reference.getId())
				.getSingleResult();
		assertThat(stored).isEqualTo("가구");
		assertThat(entityManager.find(CategoryCarbonReference.class, reference.getId()).getCategoryGroup())
				.isEqualTo(CategoryGroup.FURNITURE);
	}

}
