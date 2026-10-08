package com.Wolgyesangdan.backend.domain.verification.dto;

/**
 * 승인 요청. admissionYear는 신입생만 필요하다 (서류를 보고 관리자가 고른 입학 연도 — 그해 12월 31일까지 유효).
 * 기초수급자는 본문 없이 보낸다.
 */
public record AdminVerificationApproveRequest(Integer admissionYear) {
}
