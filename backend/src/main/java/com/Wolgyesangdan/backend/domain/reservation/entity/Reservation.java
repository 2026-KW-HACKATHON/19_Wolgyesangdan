package com.Wolgyesangdan.backend.domain.reservation.entity;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Reservation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "application_id", nullable = false, unique = true)
	private Application application;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private TradeMethod tradeMethod;

	private LocalDateTime scheduledAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private ReservationStatus status;

	private LocalDateTime reconfirmationDeadline;

	private LocalDateTime reconfirmedAt;

	private LocalDateTime completedAt;

	/**
	 * 거점 거래에서 등록자가 맡긴 물품이 거점에 들어온 시각 (#252). 아직 안 들어왔거나 직거래면 null.
	 * 신청자가 먼저 수령을 재확인하면 status가 RECONFIRMED로 바뀌어서, 입고 여부는 상태가 아니라 이 값으로 본다.
	 */
	private LocalDateTime hubReceivedAt;

	/** 물품이 거점에 들어와 있는지 */
	public boolean isAtHub() {
		return hubReceivedAt != null;
	}

	/** 거래가 끝난 예약인지 — 완료·노쇼·취소 */
	public boolean isClosed() {
		return status == ReservationStatus.COMPLETED || status == ReservationStatus.NO_SHOW
				|| status == ReservationStatus.CANCELED;
	}

	/**
	 * 거점 입고 — 운영진이 물품을 받아 두었다. 맡기기 예정이던 예약은 "거점 보관 중"으로 바꾸고,
	 * 신청자가 이미 수령을 재확인한 예약(RECONFIRMED)은 상태를 그대로 둔다.
	 */
	public void receiveAtHub(LocalDateTime now) {
		this.hubReceivedAt = now;
		if (this.status == ReservationStatus.HUB_DROP_SCHEDULED) {
			this.status = ReservationStatus.HUB_RECEIVED;
		}
	}

	/** 수령 재확인 — 가능 여부(상태·기한·본인)는 ReservationService에서 확인한다 */
	public void reconfirm(LocalDateTime now) {
		this.status = ReservationStatus.RECONFIRMED;
		this.reconfirmedAt = now;
	}

	/** 노쇼 — 재확인 기한까지 응답이 없을 때 스케줄러가, 거점에 받으러 오지 않았을 때 운영진이 표시한다 */
	public void markNoShow() {
		this.status = ReservationStatus.NO_SHOW;
	}

	/** 거래 완료 — 직거래는 등록자의 "전달 완료", 거점 거래는 운영진의 "수령 완료" (#252) */
	public void complete(LocalDateTime now) {
		this.status = ReservationStatus.COMPLETED;
		this.completedAt = now;
	}
}
