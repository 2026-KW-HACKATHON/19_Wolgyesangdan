package com.Wolgyesangdan.backend.domain.verification.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.config.S3Properties;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 우선배정 서류(합격증·학생증·수급자 증명서) S3 presigned PUT URL 발급. 방식은 물품 사진(ItemImageUploadService)과 같다.
 * 다른 점: 서류는 개인정보라 공개 URL을 만들지 않고 저장 위치(fileKey)만 돌려준다.
 * 버킷 정책에서 verifications/ 경로는 공개 읽기에서 빠져 있어야 한다.
 */
@Service
@RequiredArgsConstructor
public class VerificationDocumentUploadService {

	/** 이 서비스가 만드는 fileKey 형식. 인증 신청 때 다른 경로를 넣지 못하게 검증하는 데 쓴다 */
	public static final String FILE_KEY_PATTERN =
			"^verifications/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}\\.(jpg|png|webp|heic|heif|pdf)$";

	private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(5);
	private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	// contentType은 DocumentUploadUrlRequest에서 이 6가지로 이미 검증됐다고 가정한다
	private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp",
			"image/heic", "heic",
			"image/heif", "heif",
			"application/pdf", "pdf");

	private final S3Presigner s3Presigner;
	private final S3Properties s3Properties;

	public DocumentUploadUrlResponse issueUploadUrl(DocumentUploadUrlRequest request) {
		if (!s3Properties.isConfigured()) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_DOCUMENT_UPLOAD_UNAVAILABLE);
		}

		String fileKey = "verifications/%s/%s.%s".formatted(
				LocalDate.now().format(DATE_PATH),
				UUID.randomUUID(),
				EXTENSION_BY_CONTENT_TYPE.get(request.contentType()));

		PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(fileKey)
				.contentType(request.contentType())
				.build();
		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(UPLOAD_URL_DURATION)
				.putObjectRequest(objectRequest)
				.build();
		String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

		return new DocumentUploadUrlResponse(uploadUrl, fileKey);
	}

}
