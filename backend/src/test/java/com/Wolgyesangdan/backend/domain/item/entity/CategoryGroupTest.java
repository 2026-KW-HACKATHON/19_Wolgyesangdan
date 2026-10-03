package com.Wolgyesangdan.backend.domain.item.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class CategoryGroupTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final CategoryGroupConverter converter = new CategoryGroupConverter();

	@Test
	void 한글_표시값으로_enum을_찾는다() {
		assertThat(CategoryGroup.fromLabel("가구")).isEqualTo(CategoryGroup.FURNITURE);
		assertThat(CategoryGroup.fromLabel("기타")).isEqualTo(CategoryGroup.ETC);
	}

	@Test
	void 없는_표시값이면_예외() {
		assertThatThrownBy(() -> CategoryGroup.fromLabel("가전제품"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> CategoryGroup.fromLabel("FURNITURE"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void JSON에서는_한글_표시값으로_주고받는다() {
		assertThat(jsonMapper.writeValueAsString(CategoryGroup.APPLIANCE)).isEqualTo("\"가전\"");
		assertThat(jsonMapper.readValue("\"주방\"", CategoryGroup.class)).isEqualTo(CategoryGroup.KITCHEN);
	}

	@Test
	void DB에는_한글_표시값으로_저장한다() {
		assertThat(converter.convertToDatabaseColumn(CategoryGroup.LIVING)).isEqualTo("생활");
		assertThat(converter.convertToEntityAttribute("생활")).isEqualTo(CategoryGroup.LIVING);
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
		assertThat(converter.convertToEntityAttribute(null)).isNull();
	}

}
