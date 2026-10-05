package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

/** 거래 완료 한 건 — 월별·날짜별 묶음은 서비스에서 한다 (집계 쿼리 결과) */
public record CompletedTrade(LocalDateTime completedAt, int carbonReductionKg) {
}
