package com.Wolgyesangdan.backend.domain.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * fileName은 받지만 S3 저장 경로에는 쓰지 않는다 (한글·공백·중복 방지, 2026-10-05 결정 #50).
 */
public record ImageUploadUrlRequest(
		@NotBlank String fileName,
		@NotBlank
		@Pattern(regexp = "^(image/jpeg|image/png|image/webp|image/heic|image/heif)$",
				message = "지원하지 않는 파일 형식입니다.")
		String contentType) {
}
