package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.time.LocalDateTime;

/** 물품별 진행 중인 예약의 전달 예정 일시 */
public record ItemSchedule(Long itemId, LocalDateTime scheduledAt) {
}
