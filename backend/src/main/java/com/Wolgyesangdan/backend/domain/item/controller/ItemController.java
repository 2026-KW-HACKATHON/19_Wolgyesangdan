package com.Wolgyesangdan.backend.domain.item.controller;

import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.CategoryResponse;
import com.Wolgyesangdan.backend.domain.item.service.ItemService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

	private final ItemService itemService;

	@GetMapping("/categories")
	public List<CategoryResponse> getCategories() {
		return itemService.getCategories();
	}

}
