package com.Wolgyesangdan.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * S3_* 값이 비어 있어도(로컬에 AWS 키가 없는 팀원 환경) 서버는 정상 기동해야 해서,
 * 각 값을 @Value로 개별 주입하고 기본값을 빈 문자열로 둔다. 실제 사용 시점(ItemImageUploadService)에
 * isConfigured()로 확인한다.
 */
@Component
public class S3Properties {

	private final String bucket;
	private final String region;
	private final String accessKey;
	private final String secretKey;

	public S3Properties(
			@Value("${s3.bucket:}") String bucket,
			@Value("${s3.region:}") String region,
			@Value("${s3.access-key:}") String accessKey,
			@Value("${s3.secret-key:}") String secretKey) {
		this.bucket = bucket;
		this.region = region;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	public String bucket() {
		return bucket;
	}

	public String region() {
		return region;
	}

	public String accessKey() {
		return accessKey;
	}

	public String secretKey() {
		return secretKey;
	}

	public boolean isConfigured() {
		return !bucket.isBlank() && !region.isBlank() && !accessKey.isBlank() && !secretKey.isBlank();
	}

}
