package com.Wolgyesangdan.backend.global.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 업로드용 presigned URL 발급. 서버가 키로 URL에 서명만 하고 실제 업로드는 브라우저가 S3로 직접 한다
 * (서명은 네트워크 호출 없이 로컬에서 계산되므로 버킷이 없어도 URL은 만들어진다).
 *
 * S3 설정이 비어 있으면 서버는 그대로 뜨고 isAvailable()이 false가 된다 — AWS 키가 없는 팀원 로컬 환경 보호.
 */
@Slf4j
@Component
public class ImageStorage implements AutoCloseable {

	private final S3Properties s3Properties;
	private final S3Presigner presigner;

	public ImageStorage(S3Properties s3Properties) {
		this.s3Properties = s3Properties;
		if (!s3Properties.isConfigured()) {
			log.warn("S3 설정(S3_BUCKET/S3_ACCESS_KEY/S3_SECRET_KEY)이 비어 있어 이미지 업로드를 사용할 수 없습니다.");
			this.presigner = null;
			return;
		}
		this.presigner = S3Presigner.builder()
				.region(Region.of(s3Properties.region()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(s3Properties.accessKey(), s3Properties.secretKey())))
				.build();
	}

	public boolean isAvailable() {
		return presigner != null;
	}

	/**
	 * key 위치에 contentType 파일을 PUT할 수 있는 URL을 발급한다.
	 * contentType도 서명에 포함되므로, 업로드할 때 같은 Content-Type 헤더를 보내야 한다.
	 */
	public PresignedUpload presignPut(String key, String contentType) {
		if (!isAvailable()) {
			throw new IllegalStateException("S3가 설정되지 않았습니다.");
		}
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(key)
				.contentType(contentType)
				.build();
		String uploadUrl = presigner.presignPutObject(PutObjectPresignRequest.builder()
						.signatureDuration(s3Properties.presignExpiration())
						.putObjectRequest(putObjectRequest)
						.build())
				.url()
				.toString();
		return new PresignedUpload(uploadUrl, s3Properties.resolvedPublicBaseUrl() + "/" + key);
	}

	@Override
	public void close() {
		if (presigner != null) {
			presigner.close();
		}
	}

}
