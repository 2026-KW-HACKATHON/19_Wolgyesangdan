package com.Wolgyesangdan.backend.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
import com.Wolgyesangdan.backend.domain.reservation.dto.CategoryCarbonSum;
import com.Wolgyesangdan.backend.domain.reservation.dto.DeliveryTodo;
import com.Wolgyesangdan.backend.domain.reservation.dto.ItemSchedule;
import com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmTodo;
import com.Wolgyesangdan.backend.domain.reservation.dto.TradeCounts;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/** 신청 재신청 시 — 노쇼로 빠진(예약이 남아 있는) 신청은 지우면 안 된다 */
	boolean existsByApplicationId(Long applicationId);

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
	// 전부 거래 완료(COMPLETED)된 예약 기준이고, campaignId가 null이면 전체, 있으면 그 캠페인의 거래만 센다.

	/**
	 * "그 캠페인의 거래"인지 (#248). 둘 중 하나면 캠페인 거래다.
	 * - 그 캠페인에 연결된 물품 (거점 거래를 고른 물품)
	 * - 캠페인에 연결되지 않은 물품(직거래 전용)인데 캠페인 기간(:from 이상 :to 미만) 안에 거래가 완료된 것
	 * 다른 캠페인에 연결된 물품은 기간이 겹쳐도 세지 않는다.
	 * :to는 CampaignService.directTradePeriodEnd — 운영 중을 끈 시각이나 다음 캠페인 시작에서 끊어서,
	 * 한 직거래가 두 캠페인에 잡히지 않는다 (#250).
	 */
	String IN_CAMPAIGN = """
			(i.campaign.id = :campaignId
				or (i.campaign is null and r.completedAt >= :from and r.completedAt < :to))
			""";

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
	 * campaignId가 null이면 from·to는 쓰이지 않는다.
	 */
	@Query("""
			select coalesce(sum(i.estimatedCarbonReduction), 0L)
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and i.owner.id = :userId
				and i.owner.id <> a.applicant.id
				and (:campaignId is null or
			""" + IN_CAMPAIGN + ")")
	long sumGivenCarbonByUserId(@Param("userId") Long userId, @Param("campaignId") Long campaignId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	/**
	 * 카테고리 대분류별 거래 완료 수와 예상 탄소 절감량 합계. 거래가 없는 대분류는 결과에 없다.
	 * 전체 물품 수·합계도 이 결과를 더해서 구한다 (따로 집계 쿼리를 보내지 않음).
	 * campaignId가 null이면 from·to는 쓰이지 않는다.
	 */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.reservation.dto.CategoryCarbonSum(
				i.categoryGroup, count(r), coalesce(sum(i.estimatedCarbonReduction), 0L))
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and (:campaignId is null or
			""" + IN_CAMPAIGN + """
				)
			group by i.categoryGroup
			""")
	List<CategoryCarbonSum> sumCompletedByCategoryGroup(@Param("campaignId") Long campaignId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	/**
	 * 한 캠페인의 거래 완료 수와 예상 탄소 절감량 합계 — 홈 캠페인 배너, 관리자 대시보드·지난 캠페인 목록에 쓴다.
	 * 탄소 리포트의 "이번 캠페인"과 같은 기준이다.
	 */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary(
				count(r), coalesce(sum(i.estimatedCarbonReduction), 0L))
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and
			""" + IN_CAMPAIGN)
	CompletedItemSummary summarizeCompletedInCampaign(@Param("campaignId") Long campaignId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	/** 처음으로 거래가 완료된 일시 (월계1동 전체). 없으면 null */
	@Query("""
			select min(r.completedAt)
			from Reservation r
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
			""")
	LocalDateTime findFirstCompletedAt();

	/** 그 캠페인의 거래가 완료된 일시들 (날짜별 거래 수 집계용, 캠페인 기간만이라 건수가 적다) */
	@Query("""
			select r.completedAt
			from Reservation r
				join r.application a
				join a.item i
			where r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.COMPLETED
				and
			""" + IN_CAMPAIGN)
	List<LocalDateTime> findCompletedAtInCampaign(@Param("campaignId") Long campaignId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	/**
	 * 물품별 진행 중인 예약의 배정된 신청 id와 전달 예정 일시. 노쇼·취소된 예약은 빼고,
	 * 노쇼 승계로 예약이 여러 건이면 나중 건이 뒤에 오도록 id 순으로 준다.
	 */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.reservation.dto.ItemSchedule(
				r.application.item.id, r.application.id, r.scheduledAt)
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

	/**
	 * 승계 대상 예약 — 재확인 기한이 지났는데 아직 재확인하지 않은 예약, 또는 운영진이 노쇼로 표시했는데
	 * 아직 승계하지 않은(신청이 SELECTED로 남아 있는) 예약.
	 */
	@Query("""
			select r.id from Reservation r
			where (r.status in :reconfirmable and r.reconfirmationDeadline < :now)
				or (r.status = com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus.NO_SHOW
					and r.application.status = com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus.SELECTED)
			order by r.id
			""")
	List<Long> findIdsToSucceed(@Param("reconfirmable") Collection<ReservationStatus> reconfirmable,
			@Param("now") LocalDateTime now);

	/** 승계 처리 전에 물품부터 잠그기 위해 물품 id만 읽는다 */
	@Query("select r.application.item.id from Reservation r where r.id = :id")
	Optional<Long> findItemIdById(@Param("id") Long id);


	/**
	 * 신청자가 수령 재확인해야 할 예약 — 배정된 내 신청이고, 진행 중이며, 아직 재확인 안 했고, 기한이 남은 것.
	 * 기한 빠른 순, 기한이 없는 예약은 맨 뒤 (#187)
	 */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.reservation.dto.ReconfirmTodo(
				a.id, r.id, i.id, i.name, r.reconfirmationDeadline)
			from Reservation r
				join r.application a
				join a.item i
			where a.applicant.id = :userId
				and a.status = com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus.SELECTED
				and r.status in :statuses
				and r.reconfirmedAt is null
				and (r.reconfirmationDeadline is null or r.reconfirmationDeadline > :now)
			order by case when r.reconfirmationDeadline is null then 1 else 0 end, r.reconfirmationDeadline, r.id
			""")
	List<ReconfirmTodo> findReconfirmTodos(@Param("userId") Long userId,
			@Param("statuses") Collection<ReservationStatus> statuses, @Param("now") LocalDateTime now);

	/** 등록자가 전달해야 할 예약 — 내 물품의 예약 중 끝나지 않은(완료·노쇼·취소 아님) 것, 배정된 순 (#187) */
	@Query("""
			select new com.Wolgyesangdan.backend.domain.reservation.dto.DeliveryTodo(
				i.id, i.name, r.id, a.id, r.tradeMethod, r.status, r.reconfirmationDeadline)
			from Reservation r
				join r.application a
				join a.item i
			where i.owner.id = :userId
				and r.status in :statuses
			order by r.createdAt, r.id
			""")
	List<DeliveryTodo> findDeliveryTodos(@Param("userId") Long userId,
			@Param("statuses") Collection<ReservationStatus> statuses);

}
