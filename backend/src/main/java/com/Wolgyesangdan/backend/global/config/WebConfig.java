package com.Wolgyesangdan.backend.global.config;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	/**
	 * 쿼리 파라미터의 한글 카테고리(?categoryGroup=가구)를 CategoryGroup으로 변환한다.
	 * 없는 값이면 변환 실패 → GlobalExceptionHandler가 400 INVALID_INPUT으로 응답.
	 */
	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(String.class, CategoryGroup.class, CategoryGroup::fromLabel);
	}

}
