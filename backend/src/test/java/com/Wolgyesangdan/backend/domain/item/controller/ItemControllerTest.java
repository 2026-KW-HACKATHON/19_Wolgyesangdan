package com.Wolgyesangdan.backend.domain.item.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSearchCondition;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSort;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.service.ItemService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.config.WebConfig;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ItemController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class, WebConfig.class})
class ItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ItemService itemService;

	@Test
	void 비로그인으로_카테고리_목록을_조회한다() throws Exception {
		given(itemService.getCategories()).willReturn(List.of(
				new CategoryResponse(CategoryGroup.FURNITURE, 30),
				new CategoryResponse(CategoryGroup.APPLIANCE, 24)));

		mockMvc.perform(get("/items/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].categoryGroup").value("가구"))
				.andExpect(jsonPath("$[0].carbonReductionKg").value(30))
				.andExpect(jsonPath("$[1].categoryGroup").value("가전"))
				.andExpect(jsonPath("$[1].carbonReductionKg").value(24));
	}

	@Test
	void 비로그인으로_물품_목록을_명세의_페이지_형식으로_조회한다() throws Exception {
		ItemSummaryResponse item = new ItemSummaryResponse(1L, "전자레인지", "생활가전", CategoryGroup.APPLIANCE, "좋음",
				"https://example.com/photo1.jpg", 24, List.of(TradeMethod.DIRECT, TradeMethod.CAMPAIGN),
				ItemStatus.OPEN, 3, 5, LocalDateTime.of(2026, 10, 2, 23, 59, 59));
		given(itemService.getItems(ItemSearchCondition.none(), 0, 20)).willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(1))
				.andExpect(jsonPath("$.content[0].categoryGroup").value("가전"))
				.andExpect(jsonPath("$.content[0].thumbnailImageUrl").value("https://example.com/photo1.jpg"))
				.andExpect(jsonPath("$.content[0].tradeMethods[0]").value("DIRECT"))
				.andExpect(jsonPath("$.content[0].maxApplicants").value(5))
				.andExpect(jsonPath("$.content[0].applicationDeadline").value("2026-10-02T23:59:59"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.number").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true))
				// Spring Page 내부 필드는 나가지 않는다
				.andExpect(jsonPath("$.pageable").doesNotExist())
				.andExpect(jsonPath("$.sort").doesNotExist());
	}

	@Test
	void 범위를_벗어난_page_size는_보정한다() throws Exception {
		given(itemService.getItems(ItemSearchCondition.none(), 0, 100)).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

		mockMvc.perform(get("/items").param("page", "-1").param("size", "1000"))
				.andExpect(status().isOk());

		verify(itemService).getItems(ItemSearchCondition.none(), 0, 100);
	}

	@Test
	void 검색_조건을_한글_카테고리까지_받아서_넘긴다() throws Exception {
		ItemSearchCondition expected =
				new ItemSearchCondition("의자", CategoryGroup.FURNITURE, TradeMethod.CAMPAIGN, ItemSort.CARBON);
		given(itemService.getItems(expected, 0, 20)).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		mockMvc.perform(get("/items")
						.param("keyword", "  의자 ")
						.param("categoryGroup", "가구")
						.param("tradeMethod", "CAMPAIGN")
						.param("sort", "CARBON"))
				.andExpect(status().isOk());

		verify(itemService).getItems(expected, 0, 20);
	}

	@Test
	void 없는_카테고리면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/items").param("categoryGroup", "가전제품"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("categoryGroup"));
	}

	@Test
	void 없는_정렬_기준이면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/items").param("sort", "PRICE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("sort"));
	}

}
