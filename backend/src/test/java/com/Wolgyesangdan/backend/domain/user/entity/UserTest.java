package com.Wolgyesangdan.backend.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void 연락_수단을_바꿔도_다른_쪽_값은_보존된다() {
		User user = User.builder().kakaoId("1").nickname("테스터").build();

		user.updateContact(ContactType.PHONE, "010-1234-5678", null);
		user.updateContact(ContactType.OPENCHAT, null, "https://open.kakao.com/o/abc123");

		assertThat(user.getContactType()).isEqualTo(ContactType.OPENCHAT);
		assertThat(user.getOpenchatLink()).isEqualTo("https://open.kakao.com/o/abc123");
		assertThat(user.getPhone()).isEqualTo("010-1234-5678");
	}

	@Test
	void 선택하지_않은_쪽으로_함께_보낸_값은_무시된다() {
		User user = User.builder().kakaoId("1").nickname("테스터").build();

		user.updateContact(ContactType.PHONE, "010-1234-5678", "https://open.kakao.com/o/ignored");

		assertThat(user.getPhone()).isEqualTo("010-1234-5678");
		assertThat(user.getOpenchatLink()).isNull();
	}

}
