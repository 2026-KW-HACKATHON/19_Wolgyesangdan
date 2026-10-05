package com.Wolgyesangdan.backend.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.reservation.dto.CategoryCarbonSum;
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

	// ── 탄소 성과 집계 (carbonreport) ──
	// 전부 거래 완료(COMPLETED)된 예약 기준이고, campaignId가 null이면 전체, 있으면 그 캠페인 물품만 센다.

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
	 * 내가 등록해서(나눔) 거래 완료된 물품의 예상 탄소 절감량 합계 — 리포트의 "내가 키운 나무".
	 * 수령 쪽은 세지 않아 한 거래가 두 사람에게 중복으로 잡히지 않는다 (동네 전체 = 각자 키운 나무의 합).
	 */
	@Query("""
			select coalesce(sum(i.estimatedCarbonReduction), 0L)
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and i.owner.id = :userId
				and i.owner.id <> a.applicant.id
				and (:campaignId is null or i.campaign.id = :campaignId)
			""")
	long sumGivenCarbonByUserId(@Param("userId") Long userId, @Param("campaignId") Long campaignId);

	/**
	 * 카테고리 대분류별 거래 완료 수와 예상 탄소 절감량 합계. 거래가 없는 대분류는 결과에 없다.
	 * 전체 물품 수·합계도 이 결과를 더해서 구한다 (따로 집계 쿼리를 보내지 않음).
	 */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.reservation.dto.CategoryCarbonSum(
				i.categoryGroup, count(r), coalesce(sum(i.estimatedCarbonReduction), 0L))
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and (:campaignId is null or i.campaign.id = :campaignId)
			group by i.categoryGroup
			""")
	List<CategoryCarbonSum> sumCompletedByCategoryGroup(@Param("campaignId") Long campaignId);

	/** 처음으로 거래가 완료된 일시 (월계1동 전체). 없으면 null */
	@Query("""
			select min(r.completedAt)
			from Reservation r
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
			""")
	LocalDateTime findFirstCompletedAt();

	/** from 이후 그 캠페인에서 거래가 완료된 일시들 (날짜별 거래 수 집계용, 캠페인 기간만이라 건수가 적다) */
	@Query("""
			select r.completedAt
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and r.completedAt >= :from
				and i.campaign.id = :campaignId
			""")
	List<LocalDateTime> findCompletedAtSince(@Param("from") LocalDateTime from, @Param("campaignId") Long campaignId);

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
