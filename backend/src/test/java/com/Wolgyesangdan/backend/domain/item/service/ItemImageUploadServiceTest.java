package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.Wolgyesangdan.backend.domain.item.dto.ImageUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ImageUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.global.config.S3Properties;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class ItemImageUploadServiceTest {

	private static final ImageUploadUrlRequest REQUEST = new ImageUploadUrlRequest("photo1.jpg", "image/jpeg");

	private final S3Presigner s3Presigner = Mockito.mock(S3Presigner.class);

	@Test
	void S3가_설정되지_않았으면_503() {
		S3Properties notConfigured = new S3Properties("", "", "", "");
		ItemImageUploadService service = new ItemImageUploadService(s3Presigner, notConfigured);

		assertThatThrownBy(() -> service.issueUploadUrl(REQUEST))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_IMAGE_UPLOAD_UNAVAILABLE);
		then(s3Presigner).should(never()).presignPutObject(any(PutObjectPresignRequest.class));
	}

	@Test
	void 업로드_URL과_공개_URL을_반환한다() throws MalformedURLException {
		S3Properties configured = new S3Properties("wolgyesangdan-items", "ap-northeast-2", "access-key",
				"secret-key");
		ItemImageUploadService service = new ItemImageUploadService(s3Presigner, configured);

		PresignedPutObjectRequest presigned = Mockito.mock(PresignedPutObjectRequest.class);
		URL signedUrl = URI.create("https://wolgyesangdan-items.s3.ap-northeast-2.amazonaws.com/items/signed?X-Amz=1")
				.toURL();
		given(presigned.url()).willReturn(signedUrl);
		given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);

		ImageUploadUrlResponse response = service.issueUploadUrl(REQUEST);

		assertThat(response.uploadUrl()).isEqualTo(signedUrl.toString());
		assertThat(response.imageUrl())
				.matches("^https://wolgyesangdan-items\\.s3\\.ap-northeast-2\\.amazonaws\\.com/items/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]+\\.jpg$");
	}

	@Test
	void contentType에_맞는_확장자로_키를_만든다() {
		S3Properties configured = new S3Properties("bucket", "ap-northeast-2", "access-key", "secret-key");
		ItemImageUploadService service = new ItemImageUploadService(s3Presigner, configured);
		PresignedPutObjectRequest presigned = Mockito.mock(PresignedPutObjectRequest.class);
		given(presigned.url()).willAnswer(invocation -> URI.create("https://signed.example.com/x").toURL());
		given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);

		ImageUploadUrlResponse response = service.issueUploadUrl(new ImageUploadUrlRequest("photo.heic", "image/heic"));

		assertThat(response.imageUrl()).endsWith(".heic");
	}

}
