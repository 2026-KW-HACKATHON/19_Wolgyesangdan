package com.Wolgyesangdan.backend.domain.reservation.dto;

import java.util.List;

/**
 * 내가 지금 해야 할 일 (GET /users/me/todo, #187) — 홈 배너·마이페이지 탭 빨간 점용.
 * reconfirms: 신청자로서 수령 재확인할 것 (기한 빠른 순), deliveries: 등록자로서 전달할 것 (배정된 순).
 */
public record MyTodoResponse(List<ReconfirmTodo> reconfirms, List<DeliveryTodo> deliveries) {
}
