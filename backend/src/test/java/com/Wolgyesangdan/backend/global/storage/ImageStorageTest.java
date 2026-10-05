package com.Wolgyesangdan.backend.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * presigned URL은 네트워크 호출 없이 로컬에서 서명만 하므로 가짜 키로도 검증할 수 있다.
 */
class ImageStorageTest {

	private static S3Properties properties(String bucket, String publicBaseUrl) {
		return new S3Properties(bucket, "ap-northeast-2", "AKIAEXAMPLE", "secret-example", publicBaseUrl,
				Duration.ofMinutes(5));
	}

	@Test
	void 버킷_경로에_5분짜리_PUT_URL을_서명하고_Content_Type도_서명에_포함한다() {
		try (ImageStorage storage = new ImageStorage(properties("wolgye-bucket", ""))) {
			PresignedUpload upload = storage.presignPut("items/2026/10/05/abc.jpg", "image/jpeg");

			URI uri = URI.create(upload.uploadUrl());
			assertThat(uri.getHost()).isEqualTo("wolgye-bucket.s3.ap-northeast-2.amazonaws.com");
			assertThat(uri.getPath()).isEqualTo("/items/2026/10/05/abc.jpg");
			assertThat(uri.getQuery()).contains("X-Amz-Expires=300", "X-Amz-Signature=");
			assertThat(uri.getQuery()).containsPattern("X-Amz-SignedHeaders=[^&]*content-type");
			assertThat(upload.fileUrl())
					.isEqualTo("https://wolgye-bucket.s3.ap-northeast-2.amazonaws.com/items/2026/10/05/abc.jpg");
		}
	}

	@Test
	void 공개_주소를_따로_설정하면_그_주소로_파일_URL을_만든다() {
		try (ImageStorage storage = new ImageStorage(properties("wolgye-bucket", "https://cdn.example.com/"))) {
			assertThat(storage.presignPut("items/a.png", "image/png").fileUrl())
					.isEqualTo("https://cdn.example.com/items/a.png");
		}
	}

	@Test
	void 설정이_비어_있으면_사용_불가_상태로_뜬다() {
		try (ImageStorage storage = new ImageStorage(properties("", ""))) {
			assertThat(storage.isAvailable()).isFalse();
			assertThatThrownBy(() -> storage.presignPut("items/a.png", "image/png"))
					.isInstanceOf(IllegalStateException.class);
		}
	}

}
