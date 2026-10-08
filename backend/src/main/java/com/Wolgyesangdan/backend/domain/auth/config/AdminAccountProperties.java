package com.Wolgyesangdan.backend.domain.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 웹 로그인 계정 (POST /auth/admin/login). 운영자 한 명이 쓰는 고정 계정이라 DB가 아니라 설정값으로 둔다.
 * 로컬 기본값은 admin / 1234, 운영은 환경변수(ADMIN_LOGIN_ID, ADMIN_PASSWORD) 필수.
 */
@ConfigurationProperties(prefix = "admin")
public record AdminAccountProperties(String loginId, String password) {
}
