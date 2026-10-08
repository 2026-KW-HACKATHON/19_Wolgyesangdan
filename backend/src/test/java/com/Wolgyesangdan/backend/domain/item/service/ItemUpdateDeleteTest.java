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
import com.Wolgyesangdan.backend.domain.item.dto.MyItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemType;
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
 * 실제 MySQL에서 등록자 물품 수정·삭제 확인 — 신청자가 0명인 신청 받는 중 물품만 된다. 끝나면 전부 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ItemService.class})
class ItemUpdateDeleteTest {

	private static final LocalDate TODAY = LocalDate.now();
	private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

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
		owner = user("등록자");
	}

	@Test
	void 신청자가_없으면_수정되고_사진과_거래_방식은_보낸_목록으로_바뀐다() {
		Campaign campaign = campaign(TODAY.minusDays(1), TODAY.plusDays(5), TODAY.plusDays(7));
		Long itemId = itemService.createItem(owner.getId(), request("전자레인지", null,
				List.of(TradeMethod.DIRECT), null, List.of("https://img/1.jpg", "https://img/2.jpg"))).id();

		// 이미 있던 직거래를 그대로 다시 보내도 (item_id, trade_method) 유니크 제약에 걸리지 않아야 한다
		ItemDetailResponse updated = itemService.updateItem(owner.getId(), itemId, request(" 냉장고 ",
				ItemType.REFRIGERATOR, List.of(TradeMethod.DIRECT, TradeMethod.CAMPAIGN), campaign.getId(),
				List.of("https://img/3.jpg")));

		assertThat(updated.name()).isEqualTo("냉장고");
		assertThat(updated.itemType()).isEqualTo(ItemType.REFRIGERATOR);
		assertThat(updated.category()).isEqualTo("냉장고");
		assertThat(updated.estimatedCarbonReduction()).isEqualTo(ItemType.REFRIGERATOR.getCarbonReductionKg());
		assertThat(updated.tradeMethods()).containsExactly(TradeMethod.DIRECT, TradeMethod.CAMPAIGN);
		assertThat(updated.campaign().id()).isEqualTo(campaign.getId());
		// 거점 거래를 넣으면 신청 마감은 캠페인 신청 종료일로 바뀐다
		assertThat(updated.applicationDeadline()).isEqualTo(TODAY.plusDays(7).atTime(END_OF_DAY));
		assertThat(updated.isMine()).isTrue();

		entityManager.flush();
		entityManager.clear();
		assertThat(entityManager.createQuery("select i from ItemImage i where i.item.id = :itemId", ItemImage.class)
				.setParameter("itemId", itemId)
				.getResultList())
				.extracting(ItemImage::getImageUrl)
				.containsExactly("https://img/3.jpg");
		assertThat(itemService.getItem(itemId, owner.getId()).tradeMethods())
				.containsExactly(TradeMethod.DIRECT, TradeMethod.CAMPAIGN);
	}

	@Test
	void 직거래로_바꾸면_신청_마감은_처음_등록한_날_3일_뒤로_돌아간다() {
		Campaign campaign = campaign(TODAY.minusDays(1), TODAY.plusDays(5), TODAY.plusDays(7));
		Long itemId = itemService.createItem(owner.getId(), request("전자레인지", null,
				List.of(TradeMethod.CAMPAIGN), campaign.getId(), List.of("https://img/1.jpg"))).id();

		ItemDetailResponse updated = itemService.updateItem(owner.getId(), itemId, request("전자레인지", null,
				List.of(TradeMethod.DIRECT), campaign.getId(), List.of("https://img/1.jpg")));

		assertThat(updated.campaign()).isNull();
		assertThat(updated.applicationDeadline()).isEqualTo(TODAY.plusDays(3).atTime(END_OF_DAY));
	}

	@Test
	void 이미_연결된_캠페인이면_물품_등록_기간이_지났어도_수정할_수_있다() {
		Campaign registrationEnded = campaign(TODAY.minusDays(10), TODAY.minusDays(1), TODAY.plusDays(3));
		Item item = item(owner, registrationEnded);

		ItemDetailResponse updated = itemService.updateItem(owner.getId(), item.getId(), request("설명 고친 전자레인지",
				null, List.of(TradeMethod.CAMPAIGN), registrationEnded.getId(), List.of("https://img/1.jpg")));

		assertThat(updated.name()).isEqualTo("설명 고친 전자레인지");
		assertThat(updated.campaign().id()).isEqualTo(registrationEnded.getId());
	}

	@Test
	void 새로_거점_거래를_고르면_등록과_같이_물품_등록_기간_안이어야_한다() {
		Campaign registrationEnded = campaign(TODAY.minusDays(10), TODAY.minusDays(1), TODAY.plusDays(3));
		Item item = item(owner, null);

		assertThatThrownBy(() -> itemService.updateItem(owner.getId(), item.getId(), request("전자레인지", null,
				List.of(TradeMethod.CAMPAIGN), registrationEnded.getId(), List.of("https://img/1.jpg"))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_TRADE_METHOD_INVALID);
	}

	@Test
	void 신청자가_있으면_수정도_삭제도_할_수_없다() {
		Item item = item(owner, null);
		item.increaseApplicantCount();

		assertThatThrownBy(() -> itemService.updateItem(owner.getId(), item.getId(), directRequest()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_MODIFIABLE);
		assertThatThrownBy(() -> itemService.deleteItem(owner.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_MODIFIABLE);
	}

	@Test
	void 신청을_받는_중이_아니면_신청자가_없어도_수정할_수_없다() {
		Item item = item(owner, null);
		item.cancel();

		assertThatThrownBy(() -> itemService.updateItem(owner.getId(), item.getId(), directRequest()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_MODIFIABLE);
	}

	@Test
	void 남의_물품은_수정도_삭제도_할_수_없다() {
		Item item = item(owner, null);
		User other = user("다른 이웃");

		assertThatThrownBy(() -> itemService.updateItem(other.getId(), item.getId(), directRequest()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_OWNER);
		assertThatThrownBy(() -> itemService.deleteItem(other.getId(), item.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_OWNER);
	}

	@Test
	void 삭제하면_종료_상태가_되고_상세와_내가_등록한_물품에서_빠진다() {
		Item kept = item(owner, null);
		Item deleted = item(owner, null);

		itemService.deleteItem(owner.getId(), deleted.getId());

		entityManager.flush();
		entityManager.clear();
		Item saved = entityManager.find(Item.class, deleted.getId());
		assertThat(saved.getStatus()).isEqualTo(ItemStatus.CANCELED);
		assertThat(saved.isDeleted()).isTrue();
		assertThatThrownBy(() -> itemService.getItem(deleted.getId(), owner.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_FOUND);
		assertThat(itemService.getMyItems(owner.getId(), 0, 20).getContent())
				.extracting(MyItemSummaryResponse::id)
				.containsExactly(kept.getId());
		// 이미 삭제한 물품은 없는 물품이다
		assertThatThrownBy(() -> itemService.deleteItem(owner.getId(), deleted.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_FOUND);
	}

	private User user(String nickname) {
		User user = User.builder().kakaoId("test-" + UUID.randomUUID()).nickname(nickname).build();
		user.updateContact(ContactType.OPENCHAT, null, "https://open.kakao.com/o/abc");
		entityManager.persist(user);
		return user;
	}

	private Item item(User itemOwner, Campaign campaign) {
		Item item = Item.builder()
				.owner(itemOwner)
				.campaign(campaign)
				.name("전자레인지")
				.categoryGroup(CategoryGroup.APPLIANCE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(24)
				.applicationDeadline(TODAY.plusDays(3).atTime(END_OF_DAY))
				.status(ItemStatus.OPEN)
				.build();
		entityManager.persist(item);
		return item;
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

	private static ItemCreateRequest directRequest() {
		return request("전자레인지", null, List.of(TradeMethod.DIRECT), null, List.of("https://img/1.jpg"));
	}

	private static ItemCreateRequest request(String name, ItemType itemType, List<TradeMethod> tradeMethods,
			Long campaignId, List<String> imageUrls) {
		return new ItemCreateRequest(name, null, CategoryGroup.APPLIANCE, itemType, null, "상태 좋음", null, null, null,
				null, null, null, null, null, null, tradeMethods, campaignId, imageUrls);
	}

}
