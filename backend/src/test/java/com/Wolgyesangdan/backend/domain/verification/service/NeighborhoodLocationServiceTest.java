package com.Wolgyesangdan.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import com.Wolgyesangdan.backend.domain.verification.client.KakaoLocalClient;
import com.Wolgyesangdan.backend.domain.verification.config.NeighborhoodProperties;
import com.Wolgyesangdan.backend.domain.verification.dto.NeighborhoodLocationRequest;
import com.Wolgyesangdan.backend.domain.verification.dto.NeighborhoodLocationResponse;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.Test;

class NeighborhoodLocationServiceTest {

	private static final String WOLGYE1 = "1135056000";

	private final KakaoLocalClient kakaoLocalClient = mock(KakaoLocalClient.class);
	private final NeighborhoodLocationService service =
			new NeighborhoodLocationService(kakaoLocalClient, new NeighborhoodProperties(WOLGYE1, 100));

	@Test
	void 행정동_코드가_같으면_동네_안이다() {
		given(kakaoLocalClient.findAdministrativeRegion(37.6197, 127.059))
				.willReturn(Optional.of(new KakaoLocalClient.Region(WOLGYE1, "서울특별시 노원구 월계1동")));

		assertThat(service.check(new NeighborhoodLocationRequest(37.6197, 127.059, 20.0)))
				.isEqualTo(new NeighborhoodLocationResponse(true, "서울특별시 노원구 월계1동"));
	}

	@Test
	void 다른_행정동이면_동네_밖이고_그_동_이름을_알려준다() {
		given(kakaoLocalClient.findAdministrativeRegion(37.6542, 127.0568))
				.willReturn(Optional.of(new KakaoLocalClient.Region("1135069500", "서울특별시 노원구 상계6.7동")));

		assertThat(service.check(new NeighborhoodLocationRequest(37.6542, 127.0568, 25.0)))
				.isEqualTo(new NeighborhoodLocationResponse(false, "서울특별시 노원구 상계6.7동"));
	}

	@Test
	void 행정동이_없는_좌표면_동네_밖이다() {
		given(kakaoLocalClient.findAdministrativeRegion(0.0, 0.0)).willReturn(Optional.empty());

		assertThat(service.check(new NeighborhoodLocationRequest(0.0, 0.0, 10.0)))
				.isEqualTo(new NeighborhoodLocationResponse(false, null));
	}

	@Test
	void 오차가_기준보다_크면_카카오를_부르지_않고_VERIFICATION_LOCATION_INACCURATE() {
		assertThatThrownBy(() -> service.check(new NeighborhoodLocationRequest(37.6197, 127.059, 100.5)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_LOCATION_INACCURATE);
		verifyNoInteractions(kakaoLocalClient);
	}

	@Test
	void 오차가_기준과_같으면_판정한다() {
		given(kakaoLocalClient.findAdministrativeRegion(37.6197, 127.059))
				.willReturn(Optional.of(new KakaoLocalClient.Region(WOLGYE1, "서울특별시 노원구 월계1동")));

		assertThat(service.check(new NeighborhoodLocationRequest(37.6197, 127.059, 100.0)).inside()).isTrue();
	}

}
