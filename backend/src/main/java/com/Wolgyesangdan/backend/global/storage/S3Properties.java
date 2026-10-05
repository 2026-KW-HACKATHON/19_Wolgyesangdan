package com.Wolgyesangdan.backend.global.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 s3.* 설정. bucket/accessKey/secretKey 중 하나라도 비어 있으면 업로드 기능을 끈다.
 */
@ConfigurationProperties(prefix = "s3")
public record S3Properties(
		String bucket,
		String region,
		String accessKey,
		String secretKey,
		String publicBaseUrl,
		Duration presignExpiration) {

	public boolean isConfigured() {
		return hasText(bucket) && hasText(accessKey) && hasText(secretKey);
	}

	/** 업로드된 파일을 보여줄 기본 주소 (끝에 / 없음) */
	public String resolvedPublicBaseUrl() {
		String base = hasText(publicBaseUrl)
				? publicBaseUrl
				: "https://" + bucket + ".s3." + region + ".amazonaws.com";
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

}
