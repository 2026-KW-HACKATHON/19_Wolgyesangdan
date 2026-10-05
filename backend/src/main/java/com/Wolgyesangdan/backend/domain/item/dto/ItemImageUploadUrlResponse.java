package com.Wolgyesangdan.backend.domain.item.dto;

/**
 * @param uploadUrl 브라우저가 사진을 PUT으로 올릴 주소 (5분 유효, 요청과 같은 Content-Type 헤더 필요)
 * @param imageUrl  업로드 후 사진 주소 — 물품 등록 요청의 imageUrls에 담는다
 */
public record ItemImageUploadUrlResponse(String uploadUrl, String imageUrl) {
}
