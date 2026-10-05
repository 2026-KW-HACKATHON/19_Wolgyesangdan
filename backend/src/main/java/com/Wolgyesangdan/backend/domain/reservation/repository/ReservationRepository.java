package com.Wolgyesangdan.backend.domain.reservation.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.reservation.dto.ItemSchedule;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;

import org.springframework.data.jpa.repository.EntityGraph;
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

	/**
	 * 물품별 진행 중인 예약의 전달 예정 일시. 노쇼·취소된 예약은 빼고,
	 * 노쇼 승계로 예약이 여러 건이면 나중 건이 뒤에 오도록 id 순으로 준다.
	 */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.reservation.dto.ItemSchedule(r.application.item.id, r.scheduledAt)
			from Reservation r
			where r.application.item.id in :itemIds
				and r.status not in (
					com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.NO_SHOW,
					com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.CANCELED)
			order by r.id
			""")
	List<ItemSchedule> findActiveSchedulesByItemIdIn(@Param("itemIds") Collection<Long> itemIds);

	/** 상세 조회용 — 신청·신청자·물품·등록자를 한 번에 가져온다 */
	@EntityGraph(attributePaths = {"application.applicant", "application.item.owner"})
	Optional<Reservation> findWithParticipantsByApplicationId(Long applicationId);

	/** 재확인용 — 신청자 확인에 필요한 신청 건을 한 번에 가져온다 */
	@EntityGraph(attributePaths = "application")
	Optional<Reservation> findWithApplicationById(Long id);

}
