package com.Wolgyesangdan.backend.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 반려 요청. 사유는 회원 앱 인증 화면에 그대로 보인다 */
public record AdminVerificationRejectRequest(@NotBlank @Size(max = 255) String reason) {
}
