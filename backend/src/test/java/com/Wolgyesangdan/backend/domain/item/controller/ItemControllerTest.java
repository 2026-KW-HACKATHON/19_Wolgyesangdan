package com.Wolgyesangdan.backend.domain.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemCreateRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ItemDetailResponse;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSearchCondition;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSort;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.domain.item.service.ItemService;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.config.WebConfig;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
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

	@Autowired
	private JwtProvider jwtProvider;

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

	@Test
	void 비로그인으로_물품_상세를_조회한다() throws Exception {
		given(itemService.getItem(1L)).willReturn(new ItemDetailResponse(1L, "전자레인지", "생활가전",
				CategoryGroup.APPLIANCE, "설명", "상태 좋음", "2년 사용", false, null, "정상 작동", "48cm", "보통", 24,
				LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 4), null, LocalDateTime.of(2026, 10, 2, 23, 59, 59),
				ItemStatus.OPEN, 3, 5, List.of(TradeMethod.DIRECT),
				List.of(new ItemDetailResponse.ImageResponse("https://example.com/a.jpg", 0)),
				null, new ItemDetailResponse.OwnerInfo("월계1동 이웃", 3)));

		mockMvc.perform(get("/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.categoryGroup").value("가전"))
				.andExpect(jsonPath("$.defectYn").value(false))
				.andExpect(jsonPath("$.availableFrom").value("2026-09-20"))
				.andExpect(jsonPath("$.disposalDeadline").isEmpty())
				.andExpect(jsonPath("$.images[0].imageUrl").value("https://example.com/a.jpg"))
				.andExpect(jsonPath("$.campaign").isEmpty())
				.andExpect(jsonPath("$.owner.nickname").value("월계1동 이웃"))
				.andExpect(jsonPath("$.owner.givenCount").value(3))
				// 연락처는 노출하지 않는다
				.andExpect(jsonPath("$.owner.phone").doesNotExist())
				.andExpect(jsonPath("$.owner.openchatLink").doesNotExist());
	}

	@Test
	void 없는_물품이면_404_ITEM_NOT_FOUND() throws Exception {
		given(itemService.getItem(999L)).willThrow(new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));

		mockMvc.perform(get("/items/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
	}

	@Test
	void 물품_id가_숫자가_아니면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/items/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("itemId"));
	}

	private static final String VALID_CREATE_BODY = """
			{"name":"전자레인지","categoryGroup":"가전","conditionGrade":"상태 좋음",
			 "tradeMethods":["DIRECT"],"imageUrls":["https://img/1.jpg"]}
			""";

	@Test
	void 물품을_등록하면_201과_Location_헤더로_상세를_내려준다() throws Exception {
		given(itemService.createItem(eq(1L), any(ItemCreateRequest.class))).willReturn(detail(10L));

		createItem(VALID_CREATE_BODY)
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/items/10"))
				.andExpect(jsonPath("$.id").value(10));
	}

	@Test
	void 필수_항목이_빠지면_400() throws Exception {
		createItem("{\"name\":\"\",\"categoryGroup\":\"가전\",\"conditionGrade\":\"상태 좋음\",\"tradeMethods\":[],\"imageUrls\":[]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors.length()").value(3)); // name, tradeMethods, imageUrls
	}

	@Test
	void 사진이_5장을_넘으면_400() throws Exception {
		createItem(VALID_CREATE_BODY.replace("[\"https://img/1.jpg\"]",
						"[\"https://a/1\",\"https://a/2\",\"https://a/3\",\"https://a/4\",\"https://a/5\",\"https://a/6\"]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("imageUrls"));
	}

	@Test
	void 상태_등급이_정해진_값이_아니면_400() throws Exception {
		createItem(VALID_CREATE_BODY.replace("상태 좋음", "매우 좋음"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("conditionGrade"));
	}

	@Test
	void 본문에_없는_카테고리를_보내면_400_INVALID_INPUT과_필드명() throws Exception {
		createItem(VALID_CREATE_BODY.replace("\"가전\"", "\"가전제품\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("categoryGroup"));
	}

	@Test
	void 전달_가능_시작일이_종료일보다_늦으면_400() throws Exception {
		createItem(VALID_CREATE_BODY.replace("\"name\"",
						"\"availableFrom\":\"2026-10-10\",\"availableUntil\":\"2026-10-05\",\"name\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("availablePeriodValid"));
	}

	@Test
	void 비로그인으로_물품을_등록하면_401() throws Exception {
		mockMvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE_BODY))
				.andExpect(status().isUnauthorized());
	}

	private org.springframework.test.web.servlet.ResultActions createItem(String body) throws Exception {
		return mockMvc.perform(post("/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private static ItemDetailResponse detail(Long id) {
		return new ItemDetailResponse(id, "전자레인지", null, CategoryGroup.APPLIANCE, null, "상태 좋음", null, false,
				null, null, null, null, 24, null, null, null, LocalDateTime.of(2026, 10, 8, 23, 59, 59),
				ItemStatus.OPEN, 0, 5, List.of(TradeMethod.DIRECT),
				List.of(new ItemDetailResponse.ImageResponse("https://img/1.jpg", 0)), null,
				new ItemDetailResponse.OwnerInfo("등록자", 0));
	}

}
