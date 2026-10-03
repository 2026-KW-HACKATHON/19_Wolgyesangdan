package com.Wolgyesangdan.backend.domain.auth.client;

/**
 * 카카오에서 받아온 사용자 정보. nickname/email은 사용자가 동의하지 않으면 null.
 */
public record KakaoUser(String kakaoId, String nickname, String email) {
}
