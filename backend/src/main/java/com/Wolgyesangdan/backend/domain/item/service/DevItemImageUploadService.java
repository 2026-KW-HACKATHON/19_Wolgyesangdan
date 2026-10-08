package com.Wolgyesangdan.backend.domain.item.service;

import java.util.UUID;

import com.Wolgyesangdan.backend.domain.item.controller.DevItemImageController;
import com.Wolgyesangdan.backend.domain.item.dto.ImageUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ImageUploadUrlResponse;
import com.Wolgyesangdan.backend.global.config.S3Properties;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * ⚠️ 로컬 개발 전용. S3 설정이 비어 있으면 503 대신, 이 서버(DevItemImageController)에 올리는 URL을 발급한다.
 * 업로드 URL과 사진 주소가 같다 — 그 주소로 PUT 하면 저장되고 GET 하면 보인다.
 * S3 설정이 있으면 원래대로 S3 presigned URL을 쓴다. local 프로필에서만 등록되고, 그때는 이 빈이 우선한다.
 */
@Primary
@Profile("local")
@Service
public class DevItemImageUploadService extends ItemImageUploadService {

	private final S3Properties s3Properties;

	public DevItemImageUploadService(S3Presigner s3Presigner, S3Properties s3Properties) {
		super(s3Presigner, s3Properties);
		this.s3Properties = s3Properties;
	}

	@Override
	public ImageUploadUrlResponse issueUploadUrl(ImageUploadUrlRequest request) {
		if (s3Properties.isConfigured()) {
			return super.issueUploadUrl(request);
		}
		String fileName = UUID.randomUUID() + "."
				+ DevItemImageController.EXTENSION_BY_CONTENT_TYPE.get(request.contentType());
		String url = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/dev/images/{fileName}")
				.buildAndExpand(fileName)
				.toUriString();
		return new ImageUploadUrlResponse(url, url);
	}

}
