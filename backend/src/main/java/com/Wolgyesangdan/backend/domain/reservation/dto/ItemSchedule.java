package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

/** 물품별 진행 중인 예약 — 배정된 신청 id와 전달 예정 일시 */
public record ItemSchedule(Long itemId, Long applicationId, LocalDateTime scheduledAt) {
}
