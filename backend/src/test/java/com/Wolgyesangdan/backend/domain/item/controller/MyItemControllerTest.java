package com.Wolgyesangdan.backend.domain.item.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.MyItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
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
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = MyItemController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class, WebConfig.class})
class MyItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ItemService itemService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 내가_등록한_물품을_명세의_페이지_형식으로_조회한다() throws Exception {
		given(itemService.getMyItems(1L, 0, 20)).willReturn(new PageImpl<>(List.of(
				new MyItemSummaryResponse(2L, "1인용 책상", "https://example.com/photo2.jpg", ItemStatus.ASSIGNED, 5,
						31L, LocalDateTime.of(2026, 10, 8, 14, 0)),
				new MyItemSummaryResponse(1L, "전자레인지", null, ItemStatus.OPEN, 0, null, null)),
				PageRequest.of(0, 20), 2));

		getMyItems("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].id").value(2))
				.andExpect(jsonPath("$.content[0].name").value("1인용 책상"))
				.andExpect(jsonPath("$.content[0].thumbnailImageUrl").value("https://example.com/photo2.jpg"))
				.andExpect(jsonPath("$.content[0].status").value("ASSIGNED"))
				.andExpect(jsonPath("$.content[0].applicantCount").value(5))
				.andExpect(jsonPath("$.content[0].applicationId").value(31))
				.andExpect(jsonPath("$.content[0].scheduledAt").value("2026-10-08T14:00:00"))
				.andExpect(jsonPath("$.content[1].thumbnailImageUrl").isEmpty())
				.andExpect(jsonPath("$.content[1].applicationId").isEmpty())
				.andExpect(jsonPath("$.content[1].scheduledAt").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.number").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true))
				.andExpect(jsonPath("$.pageable").doesNotExist());
	}

	@Test
	void size는_1에서_100_사이로_보정한다() throws Exception {
		given(itemService.getMyItems(1L, 0, 100)).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

		getMyItems("?page=-1&size=1000")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(100));
	}

	@Test
	void 숫자가_아닌_page는_INVALID_INPUT() throws Exception {
		getMyItems("?page=abc")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("page"));
	}

	@Test
	void 토큰_없이_조회하면_401() throws Exception {
		mockMvc.perform(get("/users/me/items"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	private ResultActions getMyItems(String query) throws Exception {
		return mockMvc.perform(get("/users/me/items" + query)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L)));
	}

}
