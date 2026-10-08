package com.Wolgyesangdan.backend.domain.inquiry.dto;

import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
		@NotNull InquiryCategory category,
		@NotBlank @Size(max = 100) String title,
		@NotBlank @Size(max = 2000) String content) {

}
