package com.Wolgyesangdan.backend.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 우선배정 서류 업로드 URL 요청. 사진 또는 PDF 한 파일.
 * fileName은 받지만 S3 저장 경로에는 쓰지 않는다 (물품 사진과 같은 이유 — 한글·공백·중복 방지).
 */
public record DocumentUploadUrlRequest(
		@NotBlank String fileName,
		@NotBlank
		@Pattern(regexp = "^(image/jpeg|image/png|image/webp|image/heic|image/heif|application/pdf)$",
				message = "지원하지 않는 파일 형식입니다.")
		String contentType) {
}
