package com.Wolgyesangdan.backend.domain.verification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 우선배정 인증 서류 종류. 서류 파일은 받지 않고(UI 스텁) 종류만 저장한다 (2026-10-05 결정, #72).
 */
@Getter
@RequiredArgsConstructor
public enum DocumentType {

	/** 합격증 */
	ADMISSION_LETTER(VerificationType.FRESHMAN),

	/** 학생증 */
	STUDENT_ID_CARD(VerificationType.FRESHMAN),

	/** 수급자 증명서 */
	RECIPIENT_CERTIFICATE(VerificationType.LOW_INCOME);

	/** 이 서류로 신청할 수 있는 인증 유형 */
	private final VerificationType verificationType;

}
