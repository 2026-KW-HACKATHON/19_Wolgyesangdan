package com.Wolgyesangdan.backend.domain.carbonreport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.repository.CategoryCarbonReferenceRepository;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ⚠️ 로컬 시연용 — 탄소절감 리포트가 비어 보이지 않도록 거래 완료 샘플을 넣는다.
 * 거래 완료 처리 기능이 아직 없어서(요구사항 미정 사항 #5) 실제로는 COMPLETED 데이터가 생기지 않기 때문.
 *
 * 다른 테스트의 개수 검증에 영향을 주지 않도록 기본은 꺼져 있고,
 * backend/.env에 SAMPLE_CARBON_TRADES=true를 넣은 local 프로필에서만 동작한다.
 * 샘플 사용자(kakaoId sample-giver)가 이미 있으면 다시 넣지 않는다.
 */
@Slf4j
@Profile("local")
@Component
@ConditionalOnProperty(name = "SAMPLE_CARBON_TRADES", havingValue = "true")
@RequiredArgsConstructor
public class SampleCarbonTradeInitializer {

	static final String GIVER_KAKAO_ID = "sample-giver";
	static final String RECEIVER_KAKAO_ID = "sample-receiver";

	/** 최근 6개월에 흩어 넣을 거래 수와, 진행 중 캠페인 기간에 날마다 넣을 거래 수 */
	private static final int MONTHLY_TRADES = 60;
	private static final int[] CAMPAIGN_DAILY_TRADES = {6, 9, 11, 8, 14, 17, 12, 15, 19, 17, 13, 10, 16, 12, 9};
	/** 가구가 가장 많도록 대분류를 이 순서로 돌린다 */
	private static final CategoryGroup[] CATEGORY_CYCLE = {
			CategoryGroup.FURNITURE, CategoryGroup.APPLIANCE, CategoryGroup.FURNITURE, CategoryGroup.KITCHEN,
			CategoryGroup.LIVING, CategoryGroup.APPLIANCE, CategoryGroup.FURNITURE, CategoryGroup.ETC};

	private final EntityManager entityManager;
	private final UserRepository userRepository;
	private final CategoryCarbonReferenceRepository categoryCarbonReferenceRepository;
	private final CampaignService campaignService;

	// 캠페인·탄소 참조표 초기화(ApplicationRunner)가 끝난 뒤에 실행되도록 ApplicationReadyEvent를 쓴다
	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void insertSampleTrades() {
		if (userRepository.findByKakaoId(GIVER_KAKAO_ID).isPresent()) {
			return;
		}
		User giver = persist(User.builder().kakaoId(GIVER_KAKAO_ID).nickname("샘플 등록자").build());
		User receiver = persist(User.builder().kakaoId(RECEIVER_KAKAO_ID).nickname("샘플 신청자").build());
		LocalDate today = LocalDate.now();
		// 대분류별 탄소 기준값(5행)은 한 번만 읽어 둔다
		Map<CategoryGroup, Integer> carbonKgByGroup = categoryCarbonReferenceRepository.findAll().stream()
				.collect(Collectors.toMap(CategoryCarbonReference::getCategoryGroup,
						CategoryCarbonReference::getCarbonReductionKg));

		int count = 0;
		for (int i = 0; i < MONTHLY_TRADES; i++) {
			// 오래된 달일수록 드물게: 최근 180일 안에서 뒤쪽(최근)으로 몰리게 흩뿌린다
			int daysAgo = (int) (180 * Math.pow((i + 1) / (double) MONTHLY_TRADES, 1.6));
			trade(giver, receiver, null, today.minusDays(daysAgo).atTime(10 + i % 8, 0), i, carbonKgByGroup);
			count++;
		}

		Campaign campaign = campaignService.findCurrentCampaign(today)
				.filter(c -> c.statusOn(today) == CampaignStatus.ACTIVE)
				.orElse(null);
		if (campaign != null) {
			LocalDate day = campaign.periodStart();
			for (int d = 0; !day.isAfter(today) && d < CAMPAIGN_DAILY_TRADES.length; d++, day = day.plusDays(1)) {
				for (int n = 0; n < CAMPAIGN_DAILY_TRADES[d]; n++) {
					trade(giver, receiver, campaign, day.atTime(9 + n % 9, n), count++, carbonKgByGroup);
				}
			}
		}
		log.info("로컬 샘플 거래 완료 {}건 생성 (캠페인: {})", count, campaign == null ? "없음" : campaign.getId());
	}

	private void trade(User giver, User receiver, Campaign campaign, LocalDateTime completedAt, int seq,
			Map<CategoryGroup, Integer> carbonKgByGroup) {
		CategoryGroup group = CATEGORY_CYCLE[seq % CATEGORY_CYCLE.length];
		int carbonKg = carbonKgByGroup.getOrDefault(group, 10);
		Item item = persist(Item.builder()
				.owner(giver)
				.campaign(campaign)
				.name("샘플 " + group.getLabel() + " " + (seq + 1))
				.categoryGroup(group)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(carbonKg)
				.availableFrom(completedAt.toLocalDate().minusDays(7))
				.applicationDeadline(completedAt.minusDays(3))
				.status(ItemStatus.COMPLETED)
				.build());
		Application application = persist(Application.builder()
				.item(item).applicant(receiver).priorityScore(0).status(ApplicationStatus.COMPLETED).build());
		persist(Reservation.builder()
				.application(application)
				.tradeMethod(campaign == null ? TradeMethod.DIRECT : TradeMethod.CAMPAIGN)
				.status(ReservationStatus.COMPLETED)
				.completedAt(completedAt)
				.build());
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

}
