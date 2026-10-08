package com.Wolgyesangdan.backend.domain.verification.dto;

/**
 * @param inside   동네(월계1동) 안인지
 * @param dongName 현재 위치의 행정동 (예: "서울특별시 노원구 월계1동"). 행정동이 없는 좌표면 null
 */
public record NeighborhoodLocationResponse(boolean inside, String dongName) {
}
