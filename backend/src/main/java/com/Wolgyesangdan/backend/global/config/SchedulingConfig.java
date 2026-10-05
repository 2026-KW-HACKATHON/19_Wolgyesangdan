package com.Wolgyesangdan.backend.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배정·승계 스케줄러를 켠다. 테스트에서는 scheduling.enabled=false로 꺼서
 * 테스트가 쓰는 로컬 DB의 실제 데이터를 스케줄러가 건드리지 않게 한다 (build.gradle test 태스크).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
