package com.Wolgyesangdan.backend.domain.user.entity;

/**
 * 회원 역할. ADMIN은 관리자 API(/admin/**)를 쓸 수 있다.
 * 관리자 지정은 화면 없이 DB에서 직접 한다 (UPDATE users SET role = 'ADMIN' WHERE kakao_id = '...').
 */
public enum Role {
	USER,
	ADMIN
}
