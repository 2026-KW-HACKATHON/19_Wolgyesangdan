package com.Wolgyesangdan.backend.domain.verification.entity;

/**
 * 주민(RESIDENT)은 월계1동 거주 인증 — 신청 자격의 전제 조건 (예전 학생/주민 분리 인증을 통합, #59).
 * 신입생(FRESHMAN)·기초수급자(LOW_INCOME)는 주민 인증에 추가로 받는 인증으로, 물품 신청 우선순위에만 쓰인다.
 */
public enum VerificationType {
	RESIDENT,
	FRESHMAN,
	LOW_INCOME
}
