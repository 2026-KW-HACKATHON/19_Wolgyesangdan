package com.Wolgyesangdan.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.net.URI;

import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.config.S3Properties;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class VerificationDocumentUploadServiceTest {

	private final S3Presigner s3Presigner = Mockito.mock(S3Presigner.class);

	@Test
	void S3가_설정되지_않았으면_503() {
		VerificationDocumentUploadService service =
				new VerificationDocumentUploadService(s3Presigner, new S3Properties("", "", "", ""));

		assertThatThrownBy(() -> service.issueUploadUrl(new DocumentUploadUrlRequest("a.jpg", "image/jpeg")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_DOCUMENT_UPLOAD_UNAVAILABLE);
		then(s3Presigner).should(never()).presignPutObject(any(PutObjectPresignRequest.class));
	}

	@Test
	void 서류_경로에_업로드_URL을_발급하고_공개_URL_대신_fileKey를_준다() {
		VerificationDocumentUploadService service = new VerificationDocumentUploadService(s3Presigner,
				new S3Properties("wolgyesangdan-bucket", "ap-northeast-2", "access-key", "secret-key"));
		PresignedPutObjectRequest presigned = Mockito.mock(PresignedPutObjectRequest.class);
		given(presigned.url()).willAnswer(invocation -> URI.create("https://signed.example.com/x?sig=1").toURL());
		given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);

		DocumentUploadUrlResponse response =
				service.issueUploadUrl(new DocumentUploadUrlRequest("합격증 스캔.pdf", "application/pdf"));

		assertThat(response.uploadUrl()).isEqualTo("https://signed.example.com/x?sig=1");
		assertThat(response.fileKey())
				.matches(VerificationDocumentUploadService.FILE_KEY_PATTERN)
				.endsWith(".pdf");

		ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
		then(s3Presigner).should().presignPutObject(captor.capture());
		assertThat(captor.getValue().putObjectRequest().key()).isEqualTo(response.fileKey());
		assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("application/pdf");
	}

}
