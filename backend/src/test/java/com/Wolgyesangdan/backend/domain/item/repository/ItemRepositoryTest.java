package com.Wolgyesangdan.backend.domain.item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.item.dto.CompletedItemSummary;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 집계 쿼리 확인. 끝나면 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ItemRepositoryTest {

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private EntityManager entityManager;

	private User owner;
	private Campaign campaign;
	private Campaign otherCampaign;

	@BeforeEach
	void setUp() {
		owner = persist(User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("테스터").build());
		campaign = persist(campaign());
		otherCampaign = persist(campaign());
	}

	@Test
	void 캠페인의_거래완료_물품_수와_탄소_절감량_합계를_집계한다() {
		persist(item(campaign, ItemStatus.COMPLETED, 30));
		persist(item(campaign, ItemStatus.COMPLETED, 24));
		persist(item(campaign, ItemStatus.ASSIGNED, 10));       // 거래완료 아님
		persist(item(otherCampaign, ItemStatus.COMPLETED, 15)); // 다른 캠페인
		persist(item(null, ItemStatus.COMPLETED, 8));           // 직거래 전용

		assertThat(itemRepository.summarizeCompletedByCampaignId(campaign.getId()))
				.isEqualTo(new CompletedItemSummary(2, 54));
	}

	@Test
	void 거래완료_물품이_없으면_0() {
		assertThat(itemRepository.summarizeCompletedByCampaignId(campaign.getId()))
				.isEqualTo(new CompletedItemSummary(0, 0));
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private static Campaign campaign() {
		LocalDate today = LocalDate.now();
		return Campaign.builder()
				.name("테스트 캠페인")
				.registrationStartDate(today).registrationEndDate(today)
				.applicationStartDate(today).applicationEndDate(today)
				.pickupStartDate(today).pickupEndDate(today)
				.locationName("거점").locationAddress("주소")
				.status(CampaignStatus.ACTIVE)
				.build();
	}

	private Item item(Campaign campaign, ItemStatus status, int carbonReduction) {
		return Item.builder()
				.owner(owner)
				.campaign(campaign)
				.name("물품")
				.categoryGroup(CategoryGroup.ETC)
				.category("기타")
				.conditionGrade("좋음")
				.transportDifficulty("쉬움")
				.estimatedCarbonReduction(carbonReduction)
				.availableFrom(LocalDate.now())
				.availableUntil(LocalDate.now())
				.applicationDeadline(LocalDateTime.now())
				.status(status)
				.build();
	}

}
