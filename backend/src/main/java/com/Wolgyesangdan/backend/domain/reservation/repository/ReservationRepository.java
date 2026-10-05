package com.Wolgyesangdan.backend.domain.reservation.repository;

import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/**
	 * 등록자가 전달을 완료한 횟수 — 그 사람이 등록한 물품의 예약 중 COMPLETED인 것의 수.
	 * ERD 결정대로 캐시 필드 없이 조회할 때마다 계산한다.
	 */
	@Query("""
			select count(r) from Reservation r
			where r.application.item.owner.id = :ownerId
				and r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
			""")
	long countCompletedByItemOwnerId(@Param("ownerId") Long ownerId);

}
