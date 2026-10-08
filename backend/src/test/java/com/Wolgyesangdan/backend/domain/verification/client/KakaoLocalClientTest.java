package com.Wolgyesangdan.backend.domain.verification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.Wolgyesangdan.backend.domain.auth.client.KakaoProperties;
import com.Wolgyesangdan.backend.domain.verification.exception.VerificationErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoLocalClientTest {

	private static final String URI = KakaoLocalClient.COORD_TO_REGION_URI + "?x=127.059&y=37.6197";

	private MockRestServiceServer server;
	private KakaoLocalClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new KakaoLocalClient(new KakaoProperties("rest-key", "secret", "http://localhost"), builder);
	}

	@Test
	void 좌표의_행정동을_조회한다() {
		server.expect(requestTo(URI))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "KakaoAK rest-key"))
				.andRespond(withSuccess("""
						{"meta":{"total_count":2},"documents":[
						 {"region_type":"B","code":"1135010200","address_name":"서울특별시 노원구 월계동"},
						 {"region_type":"H","code":"1135056000","address_name":"서울특별시 노원구 월계1동"}]}
						""", MediaType.APPLICATION_JSON));

		assertThat(client.findAdministrativeRegion(37.6197, 127.059))
				.contains(new KakaoLocalClient.Region("1135056000", "서울특별시 노원구 월계1동"));
		server.verify();
	}

	@Test
	void 행정동이_없는_좌표면_빈_값() {
		server.expect(requestTo(URI))
				.andRespond(withSuccess("{\"meta\":{\"total_count\":0},\"documents\":[]}", MediaType.APPLICATION_JSON));

		assertThat(client.findAdministrativeRegion(37.6197, 127.059)).isEmpty();
	}

	@Test
	void 카카오가_실패하면_VERIFICATION_LOCATION_LOOKUP_FAILED() {
		server.expect(requestTo(URI)).andRespond(withServerError());

		assertThatThrownBy(() -> client.findAdministrativeRegion(37.6197, 127.059))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_LOCATION_LOOKUP_FAILED);
	}

	@Test
	void 카카오맵_사용_설정이_꺼져_있어도_VERIFICATION_LOCATION_LOOKUP_FAILED() {
		server.expect(requestTo(URI)).andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
				.body("{\"errorType\":\"NotAuthorizedError\",\"message\":\"App(...) disabled OPEN_MAP_AND_LOCAL service.\"}"));

		assertThatThrownBy(() -> client.findAdministrativeRegion(37.6197, 127.059))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(VerificationErrorCode.VERIFICATION_LOCATION_LOOKUP_FAILED);
	}

}
