package com.Wolgyesangdan.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity의 @CreatedDate/@LastModifiedDate가 동작하려면 이 설정이 켜져 있어야 한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
