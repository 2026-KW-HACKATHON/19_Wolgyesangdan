package com.Wolgyesangdan.backend.domain.item.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ItemTypeTest {

	@Test
	void 모든_품목은_이름_근거와_양수인_절감량이_있다() {
		assertThat(ItemType.values()).allSatisfy(type -> {
			assertThat(type.getLabel()).isNotBlank();
			assertThat(type.getBasis()).isNotBlank();
			assertThat(type.getCarbonReductionKg()).isPositive();
		});
	}

	@Test
	void 품목_이름은_겹치지_않는다() {
		assertThat(Arrays.stream(ItemType.values()).map(ItemType::getLabel)).doesNotHaveDuplicates();
	}

	@Test
	void 대분류별_품목을_선언_순서대로_고른다() {
		assertThat(ItemType.of(CategoryGroup.KITCHEN))
				.containsExactly(ItemType.COOKWARE, ItemType.TABLEWARE, ItemType.UTENSILS);
		assertThat(ItemType.of(CategoryGroup.ETC)).containsExactly(ItemType.BICYCLE);
	}

	@Test
	void DB_컬럼_길이_30자_안에_들어간다() {
		assertThat(ItemType.values()).allSatisfy(type -> assertThat(type.name().length()).isLessThanOrEqualTo(30));
	}

}
