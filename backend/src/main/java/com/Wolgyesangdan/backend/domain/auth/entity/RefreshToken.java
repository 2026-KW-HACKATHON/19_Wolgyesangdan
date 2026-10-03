package com.Wolgyesangdan.backend.domain.auth.entity;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발급한 refresh 토큰. 재발급(회전) 시 이전 행을 지우고 새 행을 넣는다 — 행이 없으면 폐기된 토큰.
 * 사용자당 여러 행 가능 (여러 기기 동시 로그인). 토큰 원문 대신 SHA-256 해시만 저장한다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RefreshToken extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(unique = true, nullable = false, length = 64)
	private String tokenHash;

	@Column(nullable = false)
	private LocalDateTime expiresAt;
}
