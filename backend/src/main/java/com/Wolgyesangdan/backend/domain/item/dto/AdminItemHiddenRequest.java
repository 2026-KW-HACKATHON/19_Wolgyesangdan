package com.Wolgyesangdan.backend.domain.item.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 숨기기(true) / 다시 보이기(false).
 */
public record AdminItemHiddenRequest(@NotNull Boolean hidden) {

}
