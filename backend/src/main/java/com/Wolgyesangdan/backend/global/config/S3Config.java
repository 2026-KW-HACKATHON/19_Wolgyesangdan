package com.Wolgyesangdan.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3_* 값이 비어 있어도(로컬에 AWS 키가 없는 팀원 환경) 서버는 정상 기동시킨다.
 * AwsBasicCredentials.create()는 빈 문자열도 거부해서, 설정이 없으면 더미 값으로 대신 채운다 —
 * 어차피 S3를 쓰는 서비스들이 S3Properties.isConfigured()로 먼저 막아서 이 presigner·client를 실제로 쓰지 않는다.
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

	private static final String UNCONFIGURED = "unconfigured";

	private final S3Properties s3Properties;

	@Bean
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
				.region(region())
				.credentialsProvider(credentials())
				.build();
	}

	/** presigned URL로 못 하는 서버 쪽 작업용 (보관 기간이 지난 우선배정 서류 삭제) */
	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
				.region(region())
				.credentialsProvider(credentials())
				.build();
	}

	private Region region() {
		return Region.of(s3Properties.isConfigured() ? s3Properties.region() : Region.AP_NORTHEAST_2.id());
	}

	private StaticCredentialsProvider credentials() {
		boolean configured = s3Properties.isConfigured();
		String accessKey = configured ? s3Properties.accessKey() : UNCONFIGURED;
		String secretKey = configured ? s3Properties.secretKey() : UNCONFIGURED;
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
	}

}
