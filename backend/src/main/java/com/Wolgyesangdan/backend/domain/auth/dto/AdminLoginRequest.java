package com.Wolgyesangdan.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(@NotBlank String loginId, @NotBlank String password) {
}
