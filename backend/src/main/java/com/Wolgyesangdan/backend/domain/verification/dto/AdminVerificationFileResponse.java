package com.Wolgyesangdan.backend.domain.verification.dto;

import java.time.LocalDateTime;

/**
 * 서류 열람용 임시 URL (presigned GET, 5분). 만료되면 다시 요청한다.
 * contentType으로 이미지/PDF 중 어떻게 보여줄지 정한다.
 */
public record AdminVerificationFileResponse(String url, String contentType, LocalDateTime expiresAt) {
}
