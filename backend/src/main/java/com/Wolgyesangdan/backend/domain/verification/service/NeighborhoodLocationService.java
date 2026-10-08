package com.Wolgyesangdan.backend.domain.verification.service;

import com.Wolgyesangdan.backend.domain.verification.client.KakaoLocalClient;
import com.Wolgyesangdan.backend.domain.verification.config.NeighborhoodProperties;
import com.Wolgyesangdan.backend.domain.verification.dto.NeighborhoodLocationRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.NeighborhoodLocationResponse;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 현재 위치가 동네(월계1동) 안인지 판정한다 (#278). 카카오 로컬 API로 행정동을 조회해 코드로 비교한다.
 * 좌표는 판정에만 쓰고 저장하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class NeighborhoodLocationService {

	private final KakaoLocalClient kakaoLocalClient;
	private final NeighborhoodProperties neighborhoodProperties;

	/** 오차가 기준(100m)보다 크면 400 VERIFICATION_LOCATION_INACCURATE — 동 경계 근처에서 잘못 판정하지 않도록 */
	public NeighborhoodLocationResponse check(NeighborhoodLocationRequest request) {
		if (request.accuracy() > neighborhoodProperties.maxAccuracyMeters()) {
			throw new BusinessException(VerificationErrorCode.VERIFICATION_LOCATION_INACCURATE);
		}
		return kakaoLocalClient.findAdministrativeRegion(request.lat(), request.lng())
				.map(region -> new NeighborhoodLocationResponse(
						neighborhoodProperties.regionCode().equals(region.code()), region.addressName()))
				.orElse(new NeighborhoodLocationResponse(false, null));
	}

}
