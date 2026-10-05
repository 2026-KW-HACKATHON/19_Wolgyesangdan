package com.Wolgyesangdan.backend.domain.item.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.item.dto.ImageUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ImageUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.global.config.S3Properties;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 물품 등록 폼 사진 첨부용 S3 presigned PUT URL 발급 (2026-10-05 결정, #116).
 * 서버는 파일을 직접 받지 않는다 — 프론트가 이 URL로 S3에 바로 PUT 업로드한다.
 */
@Service
@RequiredArgsConstructor
public class ItemImageUploadService {

	private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(5);
	private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	// contentType은 ImageUploadUrlRequest에서 이 5가지로 이미 검증됐다고 가정한다
	private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp",
			"image/heic", "heic",
			"image/heif", "heif");

	private final S3Presigner s3Presigner;
	private final S3Properties s3Properties;

	public ImageUploadUrlResponse issueUploadUrl(ImageUploadUrlRequest request) {
		if (!s3Properties.isConfigured()) {
			throw new BusinessException(ItemErrorCode.ITEM_IMAGE_UPLOAD_UNAVAILABLE);
		}

		// 사용자 파일명(한글·공백·중복 가능)은 경로에 쓰지 않고, 날짜+uuid로만 키를 만든다
		String key = "items/%s/%s.%s".formatted(
				LocalDate.now().format(DATE_PATH),
				UUID.randomUUID(),
				EXTENSION_BY_CONTENT_TYPE.get(request.contentType()));

		PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(key)
				.contentType(request.contentType())
				.build();
		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(UPLOAD_URL_DURATION)
				.putObjectRequest(objectRequest)
				.build();
		String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
		String imageUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(s3Properties.bucket(), s3Properties.region(),
				key);

		return new ImageUploadUrlResponse(uploadUrl, imageUrl);
	}

}
