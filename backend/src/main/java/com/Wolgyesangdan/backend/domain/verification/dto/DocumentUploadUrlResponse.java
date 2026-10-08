package com.Wolgyesangdan.backend.domain.verification.dto;

/**
 * 서류는 공개 URL이 없다 — 업로드 위치(fileKey)만 돌려주고, 인증 신청(POST /verifications)에 그대로 담는다.
 * 관리자는 열람할 때만 짧게 유효한 조회 URL을 따로 받는다.
 */
public record DocumentUploadUrlResponse(String uploadUrl, String fileKey) {
}
