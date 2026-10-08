package com.Wolgyesangdan.backend.domain.verification.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import com.Wolgyesangdan.backend.domain.verification.dto.AdminVerificationFileResponse;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.config.S3Properties;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * S3에 올라간 우선배정 서류 열람·삭제. 서류는 공개 URL이 없어서 관리자가 볼 때마다 짧게 유효한 조회 URL을 만든다.
 * 업로드 URL 발급은 VerificationDocumentUploadService.
 */
@Component
@RequiredArgsConstructor
public class VerificationDocumentStorage {

	private static final Duration VIEW_URL_DURATION = Duration.ofMinutes(5);

	// fileKey 확장자는 VerificationDocumentUploadService.FILE_KEY_PATTERN으로 이 6가지만 들어온다
	private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
			"jpg", "image/jpeg",
			"png", "image/png",
			"webp", "image/webp",
			"heic", "image/heic",
			"heif", "image/heif",
			"pdf", "application/pdf");

	private final S3Presigner s3Presigner;
	private final S3Client s3Client;
	private final S3Properties s3Properties;

	public boolean isAvailable() {
		return s3Properties.isConfigured();
	}

	/** 열람용 presigned GET (5분). S3 설정이 없으면 503 */
	public AdminVerificationFileResponse issueViewUrl(String fileKey, LocalDateTime now) {
		if (!isAvailable()) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_DOCUMENT_UNAVAILABLE);
		}
		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(VIEW_URL_DURATION)
				.getObjectRequest(GetObjectRequest.builder().bucket(s3Properties.bucket()).key(fileKey).build())
				.build();
		String url = s3Presigner.presignGetObject(presignRequest).url().toString();
		String extension = fileKey.substring(fileKey.lastIndexOf('.') + 1);
		return new AdminVerificationFileResponse(url, CONTENT_TYPE_BY_EXTENSION.get(extension),
				now.plus(VIEW_URL_DURATION));
	}

	/** 서류 파일 삭제. 이미 없는 파일이어도 S3는 성공으로 응답한다 */
	public void delete(String fileKey) {
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3Properties.bucket()).key(fileKey).build());
	}

}
