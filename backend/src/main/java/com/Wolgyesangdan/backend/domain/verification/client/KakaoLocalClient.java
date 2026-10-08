package com.Wolgyesangdan.backend.domain.verification.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.auth.client.KakaoProperties;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 로컬 API — 좌표로 행정동을 조회한다 (#278). 카카오 로그인과 같은 REST API 키를 쓴다.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide#coord-to-district
 */
@Slf4j
@Component
public class KakaoLocalClient {

	static final String COORD_TO_REGION_URI = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json";

	/** 행정동. 법정동(B)과 구분된다 */
	private static final String ADMINISTRATIVE = "H";

	private final KakaoProperties kakaoProperties;
	private final RestClient restClient;

	@Autowired
	public KakaoLocalClient(KakaoProperties kakaoProperties) {
		this(kakaoProperties, RestClient.builder().requestFactory(requestFactoryWithTimeout()));
	}

	// 테스트에서 MockRestServiceServer를 붙인 builder를 넘기기 위한 생성자
	KakaoLocalClient(KakaoProperties kakaoProperties, RestClient.Builder restClientBuilder) {
		this.kakaoProperties = kakaoProperties;
		this.restClient = restClientBuilder.build();
	}

	/**
	 * 좌표가 속한 행정동. 바다 위나 해외처럼 행정동이 없는 좌표면 빈 값.
	 * 카카오 조회에 실패하면 502 VERIFICATION_LOCATION_LOOKUP_FAILED.
	 */
	public Optional<Region> findAdministrativeRegion(double lat, double lng) {
		try {
			RegionResponse response = restClient.get()
					.uri(COORD_TO_REGION_URI + "?x={x}&y={y}", lng, lat)
					.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoProperties.restApiKey())
					.retrieve()
					.body(RegionResponse.class);
			if (response == null || response.documents() == null) {
				throw new BusinessException(VerificationErrorCode.VERIFICATION_LOCATION_LOOKUP_FAILED);
			}
			return response.documents().stream()
					.filter(document -> ADMINISTRATIVE.equals(document.regionType()))
					.findFirst()
					.map(document -> new Region(document.code(), document.addressName()));
		} catch (RestClientException e) {
			log.error("카카오 행정동 조회 실패 (카카오맵 사용 설정·앱 키 확인 필요)", e);
			throw new BusinessException(VerificationErrorCode.VERIFICATION_LOCATION_LOOKUP_FAILED);
		}
	}

	private static JdkClientHttpRequestFactory requestFactoryWithTimeout() {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(Duration.ofSeconds(5));
		return factory;
	}

	/**
	 * @param code        행정동 코드 (예: 1135056000)
	 * @param addressName 예: "서울특별시 노원구 월계1동"
	 */
	public record Region(String code, String addressName) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record RegionResponse(List<RegionDocument> documents) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record RegionDocument(
			@JsonProperty("region_type") String regionType,
			String code,
			@JsonProperty("address_name") String addressName) {
	}

}
