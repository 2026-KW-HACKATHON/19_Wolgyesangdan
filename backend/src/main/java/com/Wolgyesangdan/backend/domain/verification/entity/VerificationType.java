package com.Wolgyesangdan.backend.domain.verification.entity;

/**
 * 이웃 인증 유형 (2026-10-05 로그인·인증 디자인 수정, #72).
 * NEIGHBORHOOD는 나눔 신청 자격, FRESHMAN·LOW_INCOME은 우선배정 가산점용이다.
 */
public enum VerificationType {

	/** GPS 동네 인증 — 월계1동 안인지는 프론트가 판정하고, 심사 없이 바로 승인된다 */
	NEIGHBORHOOD,

	/** 신입생 — 올해 광운대 입학. 서류를 보고 운영진이 판정한다 */
	FRESHMAN,

	/** 기초수급자 — 본인 외에는 어떤 응답에도 노출하지 않는다 (대기 순번에만 반영) */
	LOW_INCOME;

	/** 우선배정 가산점을 주는 유형인지 */
	public boolean isPriority() {
		return this != NEIGHBORHOOD;
	}

}
