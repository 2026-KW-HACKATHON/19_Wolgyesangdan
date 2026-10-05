package com.Wolgyesangdan.backend.domain.user.entity;

import com.Wolgyesangdan.backend.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, length = 100)
	private String kakaoId;

	@Column(length = 255)
	private String email;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(length = 20)
	private String phone;

	@Column(length = 255)
	private String openchatLink;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 30)
	private ContactType contactType;

	/**
	 * 공개할 연락 수단을 바꾸고 그 값을 갱신한다. 다른 쪽 값은 지우지 않고 보존한다 —
	 * 오픈채팅으로 바꿨다가 다시 전화번호로 돌아와도 이전에 입력한 번호가 남아있게 하기 위함.
	 */
	public void updateContact(ContactType contactType, String phone, String openchatLink) {
		this.contactType = contactType;
		switch (contactType) {
			case PHONE -> this.phone = phone;
			case OPENCHAT -> this.openchatLink = openchatLink;
		}
	}
}
