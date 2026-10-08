package com.Wolgyesangdan.backend.domain.verification.controller;

import java.util.List;

import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.DocumentUploadUrlResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.MyVerificationResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.NeighborhoodLocationRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.NeighborhoodLocationResponse;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.VerificationCreateResponse;
import com.Wolgyesangdan.backend.domain.verification.service.NeighborhoodLocationService;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationDocumentUploadService;
import com.Wolgyesangdan.backend.domain.verification.service.VerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verifications")
@RequiredArgsConstructor
public class VerificationController {

	private final VerificationService verificationService;
	private final VerificationDocumentUploadService verificationDocumentUploadService;
	private final NeighborhoodLocationService neighborhoodLocationService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VerificationCreateResponse createVerification(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody VerificationCreateRequest request) {
		return verificationService.createVerification(userId, request);
	}

	/** GPS 동네 인증. 현재 좌표로 서버가 월계1동 안인지 판정하고(#279), 안이면 심사 없이 바로 APPROVED */
	@PostMapping("/neighborhood")
	@ResponseStatus(HttpStatus.CREATED)
	public VerificationCreateResponse verifyNeighborhood(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody NeighborhoodLocationRequest request) {
		return verificationService.verifyNeighborhood(userId, request);
	}

	/**
	 * 현재 위치가 월계1동 안인지 확인 (#278). 인증을 기록하지 않고 판정 결과만 돌려준다 — 동네 인증 화면의 결과 표시용.
	 * 좌표가 URL·로그에 남지 않도록 POST 본문으로 받는다.
	 */
	@PostMapping("/neighborhood/check")
	public NeighborhoodLocationResponse checkNeighborhood(@Valid @RequestBody NeighborhoodLocationRequest request) {
		return neighborhoodLocationService.check(request);
	}

	/**
	 * 서류 파일 업로드용 presigned URL 발급. 받은 fileKey를 인증 신청(POST /verifications)에 담는다.
	 * S3 설정이 비어 있으면 503.
	 */
	@PostMapping("/documents/upload-url")
	public DocumentUploadUrlResponse issueDocumentUploadUrl(@Valid @RequestBody DocumentUploadUrlRequest request) {
		return verificationDocumentUploadService.issueUploadUrl(request);
	}

	@GetMapping("/me")
	public List<MyVerificationResponse> getMyVerifications(@AuthenticationPrincipal Long userId) {
		return verificationService.getMyVerifications(userId);
	}

}
