package com.Wolgyesangdan.backend.domain.verification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GPS 동네 인증 기준 (#278).
 *
 * @param regionCode        동네로 인정하는 행정동 코드 (카카오 로컬 API의 region_type H 코드). 월계1동은 1135056000
 * @param maxAccuracyMeters 이보다 측위 오차가 크면 판정하지 않는다
 */
@ConfigurationProperties(prefix = "neighborhood")
public record NeighborhoodProperties(String regionCode, int maxAccuracyMeters) {
}
