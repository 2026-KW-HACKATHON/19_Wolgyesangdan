package com.Wolgyesangdan.backend.domain.item.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * ⚠️ 로컬 개발 전용. S3 키가 없는 로컬 환경에서 물품 사진을 S3 대신 이 서버의 임시 폴더에 올리고 보여준다.
 * 업로드 URL은 DevItemImageUploadService가 발급한다. local 프로필에서만 등록된다.
 */
@Profile("local")
@RestController
@RequestMapping("/dev/images")
public class DevItemImageController {

	/** 업로드 URL 발급 요청의 contentType별 확장자 (ImageUploadUrlRequest에서 이 5가지로 검증된다) */
	public static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp",
			"image/heic", "heic",
			"image/heif", "heif");

	// 발급한 파일명(uuid.확장자)만 받는다 — 경로 조작(../ 등)을 막는다
	private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|webp|heic|heif)$");
	private static final Path DIRECTORY = Path.of(System.getProperty("java.io.tmpdir"), "wolgyesangdan-dev-images");

	/** 프론트가 S3 presigned URL에 하듯 파일 본문을 그대로 PUT 한다 */
	@PutMapping("/{fileName}")
	public ResponseEntity<Void> upload(@PathVariable String fileName, @RequestBody byte[] body) throws IOException {
		Path path = resolve(fileName);
		Files.createDirectories(DIRECTORY);
		Files.write(path, body);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{fileName}")
	public ResponseEntity<Resource> download(@PathVariable String fileName) {
		Path path = resolve(fileName);
		if (!Files.exists(path)) {
			return ResponseEntity.notFound().build();
		}
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
		String contentType = EXTENSION_BY_CONTENT_TYPE.entrySet().stream()
				.filter(entry -> entry.getValue().equals(extension))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(new FileSystemResource(path));
	}

	private static Path resolve(String fileName) {
		if (!FILE_NAME.matcher(fileName).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		return DIRECTORY.resolve(fileName);
	}

}
