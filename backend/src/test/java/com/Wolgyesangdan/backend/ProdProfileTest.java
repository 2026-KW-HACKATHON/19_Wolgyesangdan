package com.Wolgyesangdan.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Wolgyesangdan.backend.domain.auth.controller.DevAuthController;
import com.Wolgyesangdan.backend.domain.campaign.service.SampleCampaignInitializer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 운영(prod) 프로필로 서버가 뜨는지, 로컬 전용 기능이 꺼지는지 확인.
 * DB·JWT·카카오 값은 CI·로컬과 같은 환경변수를 쓰고, 운영에서만 필수인 값은 여기서 채운다.
 */
@SpringBootTest(properties = {
		"KAKAO_REDIRECT_URI=https://wolgyesangdan.example/oauth/kakao/callback",
		"CORS_ALLOWED_ORIGINS=https://wolgyesangdan.example",
		"ADMIN_LOGIN_ID=prod-admin", "ADMIN_PASSWORD=prod-admin-password"})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Environment environment;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 로컬_전용_기능은_빈이_만들어지지_않는다() {
		assertThat(context.getBeanNamesForType(DevAuthController.class)).isEmpty();
		assertThat(context.getBeanNamesForType(SampleCampaignInitializer.class)).isEmpty();
	}

	@Test
	void 개발용_로그인은_호출할_수_없다() throws Exception {
		mockMvc.perform(post("/dev/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"kakaoId\":\"x\",\"nickname\":\"x\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void 헬스_체크는_토큰_없이_UP만_내려준다() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void 헬스_체크_외의_actuator는_공개하지_않는다() throws Exception {
		mockMvc.perform(get("/actuator/env"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 운영_설정값이_적용된다() {
		assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("false");
		assertThat(environment.getProperty("cors.allowed-origins")).isEqualTo("https://wolgyesangdan.example");
		assertThat(environment.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
	}

}
