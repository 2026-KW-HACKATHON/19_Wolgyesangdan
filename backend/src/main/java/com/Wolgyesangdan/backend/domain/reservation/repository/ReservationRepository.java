package com.Wolgyesangdan.backend.domain.reservation.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.reservation.dto.ItemSchedule;
import com.Wolgyesangdan.backend.domain.reservation.dto.TradeCounts;
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
	 * 한 사용자의 거래 완료 집계 — 거래 완료된 예약 중 그 사람이 등록자(전달)인 것과 신청자(수령)인 것의 수,
	 * 그리고 그 물품들의 예상 탄소 절감량 합계를 쿼리 한 번으로 계산한다.
	 * 등록자와 신청자가 같은 예약(본인 물품 신청)은 전달·수령이 이중으로 잡히지 않도록 뺀다.
	 */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.reservation.dto.TradeCounts(
				coalesce(sum(case when i.owner.id = :userId then 1 else 0 end), 0L),
				coalesce(sum(case when a.applicant.id = :userId then 1 else 0 end), 0L),
				coalesce(sum(i.estimatedCarbonReduction), 0L))
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and (i.owner.id = :userId or a.applicant.id = :userId)
				and i.owner.id <> a.applicant.id
			""")
	TradeCounts summarizeTradesByUserId(@Param("userId") Long userId);

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
