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

	/** 수령 재확인 — 가능 여부(상태·기한·본인)는 ReservationService에서 확인한다 */
	public void reconfirm(LocalDateTime now) {
		this.status = ReservationStatus.RECONFIRMED;
		this.reconfirmedAt = now;
	}
}
