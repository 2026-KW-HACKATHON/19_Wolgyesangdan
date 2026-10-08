package com.Wolgyesangdan.backend.domain.item.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.AdminItemResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.item.service.AdminItemService;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
import com.Wolgyesangdan.backend.global.config.SecurityConfig;
import com.Wolgyesangdan.backend.global.config.WebConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = AdminItemController.class,
		properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class, WebConfig.class})
class AdminItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminItemService adminItemService;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 관리자가_물품_목록을_페이지_형식으로_조회한다() throws Exception {
		given(adminItemService.getItems(null, 0, 20)).willReturn(new PageImpl<>(
				List.of(item(1L, true), item(2L, false)), PageRequest.of(0, 20), 2));

		mockMvc.perform(get("/admin/items").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(1))
				.andExpect(jsonPath("$.content[0].name").value("전자레인지"))
				.andExpect(jsonPath("$.content[0].ownerNickname").value("등록자"))
				.andExpect(jsonPath("$.content[0].categoryGroup").value("가전"))
				.andExpect(jsonPath("$.content[0].status").value("OPEN"))
				.andExpect(jsonPath("$.content[0].createdAt").value("2026-10-08T10:00:00"))
				.andExpect(jsonPath("$.content[0].hidden").value(true))
				.andExpect(jsonPath("$.content[1].hidden").value(false))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.number").value(0))
				.andExpect(jsonPath("$.size").value(20));
	}

	@Test
	void hidden_필터와_보정한_page_size를_넘긴다() throws Exception {
		given(adminItemService.getItems(true, 0, 100)).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

		mockMvc.perform(get("/admin/items")
						.header(HttpHeaders.AUTHORIZATION, adminToken())
						.param("hidden", "true").param("page", "-1").param("size", "1000"))
				.andExpect(status().isOk());

		verify(adminItemService).getItems(true, 0, 100);
	}

	@Test
	void 물품을_숨긴다() throws Exception {
		given(adminItemService.changeHidden(1L, true)).willReturn(item(1L, true));

		changeHidden(1L, "{\"hidden\":true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.hidden").value(true));
	}

	@Test
	void 숨긴_물품을_다시_보이게_한다() throws Exception {
		given(adminItemService.changeHidden(1L, false)).willReturn(item(1L, false));

		changeHidden(1L, "{\"hidden\":false}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hidden").value(false));
	}

	@Test
	void hidden이_빠지면_400_INVALID_INPUT() throws Exception {
		changeHidden(1L, "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("hidden"));
	}

	@Test
	void 없는_물품이면_404_ITEM_NOT_FOUND() throws Exception {
		given(adminItemService.changeHidden(999L, true)).willThrow(new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));

		changeHidden(999L, "{\"hidden\":true}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
	}

	@Test
	void 비로그인이면_401() throws Exception {
		mockMvc.perform(get("/admin/items"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 일반_회원이면_403_AUTH_FORBIDDEN() throws Exception {
		mockMvc.perform(get("/admin/items")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(7L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
		mockMvc.perform(patch("/admin/items/1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(7L))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"hidden\":true}"))
				.andExpect(status().isForbidden());
	}

	private ResultActions changeHidden(Long itemId, String body) throws Exception {
		return mockMvc.perform(patch("/admin/items/" + itemId)
				.header(HttpHeaders.AUTHORIZATION, adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String adminToken() {
		return "Bearer " + jwtProvider.createAccessToken(1L, Role.ADMIN);
	}

	private static AdminItemResponse item(Long id, boolean hidden) {
		return new AdminItemResponse(id, "전자레인지", "등록자", CategoryGroup.APPLIANCE, ItemStatus.OPEN,
				LocalDateTime.of(2026, 10, 8, 10, 0, 0), hidden);
	}

}
