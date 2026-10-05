package com.Wolgyesangdan.backend.domain.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * fileName은 명세상 받지만 저장 경로에는 쓰지 않는다 (한글·공백·중복 파일명 문제 방지 — 경로는 서버가 UUID로 만든다).
 */
public record ItemImageUploadUrlRequest(
		@NotBlank @Size(max = 255) String fileName,
		@NotBlank
		@Pattern(regexp = "^image/(jpeg|png|webp|heic|heif)$",
				message = "jpeg, png, webp, heic 형식의 이미지만 올릴 수 있습니다.")
		String contentType) {
}
