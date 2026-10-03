package com.Wolgyesangdan.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(@NotBlank String authorizationCode) {
}
