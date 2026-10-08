package com.Wolgyesangdan.backend.global.security;

import com.Wolgyesangdan.backend.domain.user.entity.Role;

/** 검증된 access 토큰에서 꺼낸 값 */
public record AccessTokenPayload(Long userId, Role role) {
}
