package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.item.dto.ItemCreateRequest;
import com.Wolgyesangdan.backend.domain.item.dto.ItemDetailResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 물품 등록 확인. 탄소 참조표는 테스트 값으로 바꿔서 쓰고, 끝나면 전부 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ItemService.class})
class ItemCreateTest {

	private static final LocalDate TODAY = LocalDate.now();

	@Autowired
	private ItemService itemService;

	@Autowired
	private EntityManager entityManager;

	private User owner;

	@BeforeEach
	void setUp() {
		entityManager.createQuery("delete from CategoryCarbonReference").executeUpdate();
		entityManager.persist(CategoryCarbonReference.builder()
				.categoryGroup(CategoryGroup.APPLIANCE).carbonReductionKg(24).build());
		owner = User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("등록자").build();
		owner.updateContact(ContactType.OPENCHAT, null, "https://open.kakao.com/o/abc");
		entityManager.persist(owner);
	}

	@Test
	void 직거래_물품은_등록일_3일_뒤_23시59분59초에_마감되고_바로_OPEN이다() {
		ItemDetailResponse response = itemService.createItem(owner.getId(),
				request(List.of(TradeMethod.DIRECT), null, List.of("https://img/2.jpg", "https://img/1.jpg")));

		assertThat(response.status()).isEqualTo(ItemStatus.OPEN);
		assertThat(response.applicationDeadline()).isEqualTo(TODAY.plusDays(3).atTime(LocalTime.of(23, 59, 59)));
		assertThat(response.estimatedCarbonReduction()).isEqualTo(24);
		assertThat(response.campaign()).isNull();
		assertThat(response.applicantCount()).isZero();
		assertThat(response.images()).extracting(ItemDetailResponse.ImageResponse::imageUrl,
						ItemDetailResponse.ImageResponse::displayOrder)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("https://img/2.jpg", 0),
						org.assertj.core.groups.Tuple.tuple("https://img/1.jpg", 1));

		entityManager.flush();
		entityManager.clear();
		Item saved = entityManager.find(Item.class, response.id());
		assertThat(saved.getOwner().getId()).isEqualTo(owner.getId());
		assertThat(saved.getCategory()).isNull();          // 선택 항목은 비워도 저장된다
		assertThat(saved.getTransportDifficulty()).isNull();
		assertThat(saved.getAvailableFrom()).isNull();
	}

	@Test
	void 거점_거래를_포함하면_캠페인_신청_종료일에_마감되고_캠페인이_연결된다() {
		Campaign campaign = campaign(TODAY.minusDays(1), TODAY.plusDays(5), TODAY.plusDays(7));

		ItemDetailResponse response = itemService.createItem(owner.getId(),
				request(List.of(TradeMethod.CAMPAIGN, TradeMethod.DIRECT, TradeMethod.CAMPAIGN), campaign.getId(),
						List.of("https://img/1.jpg")));

		assertThat(response.applicationDeadline()).isEqualTo(TODAY.plusDays(7).atTime(LocalTime.of(23, 59, 59)));
		assertThat(response.campaign().id()).isEqualTo(campaign.getId());
		assertThat(response.tradeMethods()).containsExactly(TradeMethod.DIRECT, TradeMethod.CAMPAIGN); // 중복 제거
	}

	@Test
	void 캠페인_물품_등록_기간이_아니면_거점_거래_불가() {
		Campaign ended = campaign(TODAY.minusDays(10), TODAY.minusDays(1), TODAY.plusDays(3));

		assertThatThrownBy(() -> itemService.createItem(owner.getId(),
				request(List.of(TradeMethod.CAMPAIGN), ended.getId(), List.of("https://img/1.jpg"))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_TRADE_METHOD_INVALID);
	}

	@Test
	void 거점_거래인데_캠페인을_안_보내면_불가() {
		assertThatThrownBy(() -> itemService.createItem(owner.getId(),
				request(List.of(TradeMethod.CAMPAIGN), null, List.of("https://img/1.jpg"))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_TRADE_METHOD_INVALID);
	}

	@Test
	void 직거래만이면_campaignId를_보내도_캠페인을_연결하지_않는다() {
		Campaign campaign = campaign(TODAY.minusDays(1), TODAY.plusDays(5), TODAY.plusDays(7));

		ItemDetailResponse response = itemService.createItem(owner.getId(),
				request(List.of(TradeMethod.DIRECT), campaign.getId(), List.of("https://img/1.jpg")));

		assertThat(response.campaign()).isNull();
	}

	@Test
	void 연락_수단을_설정하지_않았으면_등록_불가() {
		User noContact = User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("미설정").build();
		entityManager.persist(noContact);

		assertThatThrownBy(() -> itemService.createItem(noContact.getId(),
				request(List.of(TradeMethod.DIRECT), null, List.of("https://img/1.jpg"))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_CONTACT_NOT_SET);
	}

	@Test
	void 하자가_없으면_하자_설명은_저장하지_않는다() {
		ItemCreateRequest base = request(List.of(TradeMethod.DIRECT), null, List.of("https://img/1.jpg"));
		ItemCreateRequest withStrayDefectText = new ItemCreateRequest(base.name(), base.category(),
				base.categoryGroup(), base.description(), base.conditionGrade(), base.usagePeriod(), false,
				"실수로 남은 설명", base.workingStatus(), base.size(), base.transportDifficulty(), base.availableFrom(),
				base.availableUntil(), base.disposalDeadline(), base.tradeMethods(), base.campaignId(),
				base.imageUrls());

		assertThat(itemService.createItem(owner.getId(), withStrayDefectText).defectDescription()).isNull();
	}

	private Campaign campaign(LocalDate registrationStart, LocalDate registrationEnd, LocalDate applicationEnd) {
		Campaign campaign = Campaign.builder()
				.name("테스트 캠페인")
				.registrationStartDate(registrationStart).registrationEndDate(registrationEnd)
				.applicationStartDate(registrationStart).applicationEndDate(applicationEnd)
				.pickupStartDate(registrationStart).pickupEndDate(applicationEnd.plusDays(2))
				.locationName("거점").locationAddress("주소")
				.status(CampaignStatus.ACTIVE)
				.build();
		entityManager.persist(campaign);
		return campaign;
	}

	private static ItemCreateRequest request(List<TradeMethod> tradeMethods, Long campaignId, List<String> imageUrls) {
		return new ItemCreateRequest("전자레인지", null, CategoryGroup.APPLIANCE, null, "상태 좋음", null, null, null,
				null, null, null, null, null, null, tradeMethods, campaignId, imageUrls);
	}

}
