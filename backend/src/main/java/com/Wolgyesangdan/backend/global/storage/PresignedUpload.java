package com.Wolgyesangdan.backend.global.storage;

/**
 * @param uploadUrl 브라우저가 파일을 PUT으로 올릴 서명된 URL (짧은 유효시간)
 * @param fileUrl   업로드 후 파일을 보여줄 주소
 */
public record PresignedUpload(String uploadUrl, String fileUrl) {
}
