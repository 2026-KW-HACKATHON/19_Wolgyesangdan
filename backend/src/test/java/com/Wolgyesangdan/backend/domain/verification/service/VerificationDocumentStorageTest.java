package com.Wolgyesangdan.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.net.URI;
import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationFileResponse;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.config.S3Properties;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class VerificationDocumentStorageTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 14, 0);
	private static final String FILE_KEY = "verifications/2026/10/08/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.pdf";

	private final S3Presigner s3Presigner = Mockito.mock(S3Presigner.class);
	private final S3Client s3Client = Mockito.mock(S3Client.class);

	@Test
	void 서류_열람_URL은_5분짜리_presigned_GET이다() {
		VerificationDocumentStorage storage = configured();
		PresignedGetObjectRequest presigned = Mockito.mock(PresignedGetObjectRequest.class);
		given(presigned.url()).willAnswer(invocation -> URI.create("https://signed.example.com/doc?sig=1").toURL());
		given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presigned);

		AdminVerificationFileResponse response = storage.issueViewUrl(FILE_KEY, NOW);

		assertThat(response.url()).isEqualTo("https://signed.example.com/doc?sig=1");
		assertThat(response.contentType()).isEqualTo("application/pdf");
		assertThat(response.expiresAt()).isEqualTo(NOW.plusMinutes(5));
		ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
		then(s3Presigner).should().presignGetObject(captor.capture());
		assertThat(captor.getValue().getObjectRequest().key()).isEqualTo(FILE_KEY);
		assertThat(captor.getValue().signatureDuration()).hasMinutes(5);
	}

	@Test
	void S3가_설정되지_않았으면_열람은_503() {
		VerificationDocumentStorage storage =
				new VerificationDocumentStorage(s3Presigner, s3Client, new S3Properties("", "", "", ""));

		assertThatThrownBy(() -> storage.issueViewUrl(FILE_KEY, NOW))
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_DOCUMENT_UNAVAILABLE);
	}

	@Test
	void 서류를_지운다() {
		configured().delete(FILE_KEY);

		ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		then(s3Client).should().deleteObject(captor.capture());
		assertThat(captor.getValue().bucket()).isEqualTo("wolgyesangdan-bucket");
		assertThat(captor.getValue().key()).isEqualTo(FILE_KEY);
	}

	private VerificationDocumentStorage configured() {
		return new VerificationDocumentStorage(s3Presigner, s3Client,
				new S3Properties("wolgyesangdan-bucket", "ap-northeast-2", "access-key", "secret-key"));
	}

}
