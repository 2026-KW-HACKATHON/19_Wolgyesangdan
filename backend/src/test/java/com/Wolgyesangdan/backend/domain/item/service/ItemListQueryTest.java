package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.item.dto.ItemAvailability;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSearchCondition;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSort;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSummaryResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemImage;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

/**
 * 실제 MySQL에서 물품 목록 조회 확인. 각 테스트는 물품 관련 테이블을 비운 상태에서 시작하고, 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ItemService.class})
class ItemListQueryTest {

	@Autowired
	private ItemService itemService;

	@Autowired
	private EntityManager entityManager;

	private User owner;

	@BeforeEach
	void setUp() {
		for (String entity : List.of("Reservation", "Application", "ItemImage", "ItemTradeMethod", "Item")) {
			entityManager.createQuery("delete from " + entity).executeUpdate();
		}
		owner = persist(User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("테스터").build());
	}

	@Test
	void ALL이면_취소와_등록대기만_빼고_최신순으로_내려준다() {
		Item open = persist(item("열림", ItemStatus.OPEN));
		persist(item("등록대기", ItemStatus.REGISTERED));
		Item closed = persist(item("마감", ItemStatus.CLOSED));
		persist(item("취소", ItemStatus.CANCELED));
		Item assigned = persist(item("예약중", ItemStatus.ASSIGNED));
		Item completed = persist(item("완료", ItemStatus.COMPLETED));
		flushAndClear();

		Page<ItemSummaryResponse> page = itemService.getItems(availability(ItemAvailability.ALL), 0, 20);

		assertThat(page.getContent()).extracting(ItemSummaryResponse::id)
				.containsExactly(completed.getId(), assigned.getId(), closed.getId(), open.getId());
		assertThat(page.getTotalElements()).isEqualTo(4);
	}

	@Test
	void 기본_ACTIVE는_거래가_끝난_물품을_빼고_신청_가능과_마감만_내려준다() {
		Item open = persist(item("열림", ItemStatus.OPEN));
		Item closed = persist(item("마감", ItemStatus.CLOSED));
		persist(item("예약중", ItemStatus.ASSIGNED));
		persist(item("완료", ItemStatus.COMPLETED));
		persist(item("취소", ItemStatus.CANCELED));
		flushAndClear();

		Page<ItemSummaryResponse> page = itemService.getItems(ItemSearchCondition.none(), 0, 20);

		assertThat(page.getContent()).extracting(ItemSummaryResponse::id).containsExactly(closed.getId(), open.getId());
		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	void OPEN이면_신청_가능한_물품만_내려준다() {
		Item open = persist(item("열림", ItemStatus.OPEN));
		persist(item("마감", ItemStatus.CLOSED));
		persist(item("예약중", ItemStatus.ASSIGNED));
		persist(item("완료", ItemStatus.COMPLETED));
		flushAndClear();

		Page<ItemSummaryResponse> page = itemService.getItems(availability(ItemAvailability.OPEN), 0, 20);

		assertThat(page.getContent()).extracting(ItemSummaryResponse::id).containsExactly(open.getId());
	}

	@Test
	void 상태_범위와_다른_검색_조건을_함께_적용한다() {
		persist(item("의자 열림", ItemStatus.OPEN));
		persist(item("의자 완료", ItemStatus.COMPLETED));
		persist(item("책상 열림", ItemStatus.OPEN));
		flushAndClear();

		assertThat(names(new ItemSearchCondition("의자", null, null, ItemSort.LATEST, ItemAvailability.OPEN)))
				.containsExactly("의자 열림");
		assertThat(names(new ItemSearchCondition("의자", null, null, ItemSort.LATEST, ItemAvailability.ALL)))
				.containsExactly("의자 완료", "의자 열림");
	}

	@Test
	void 대표_사진은_노출_순서가_가장_앞인_사진이고_거래_방식을_함께_내려준다() {
		Item withPhotos = persist(item("사진 있음", ItemStatus.OPEN));
		persist(image(withPhotos, "second.jpg", 1));
		persist(image(withPhotos, "first.jpg", 0));
		persist(tradeMethod(withPhotos, TradeMethod.CAMPAIGN));
		persist(tradeMethod(withPhotos, TradeMethod.DIRECT));
		Item noPhoto = persist(item("사진 없음", ItemStatus.OPEN));
		flushAndClear();

		List<ItemSummaryResponse> content = itemService.getItems(ItemSearchCondition.none(), 0, 20).getContent();

		assertThat(content).extracting(ItemSummaryResponse::id, ItemSummaryResponse::thumbnailImageUrl,
						ItemSummaryResponse::tradeMethods)
				.containsExactly(
						tuple(noPhoto.getId(), null, List.of()),
						tuple(withPhotos.getId(), "first.jpg", List.of(TradeMethod.DIRECT, TradeMethod.CAMPAIGN)));
		assertThat(content.get(1).maxApplicants()).isEqualTo(Item.MAX_APPLICANTS);
	}

	@Test
	void 페이지_단위로_나눠서_내려준다() {
		for (int i = 0; i < 5; i++) {
			persist(item("물품" + i, ItemStatus.OPEN));
		}
		flushAndClear();

		Page<ItemSummaryResponse> page = itemService.getItems(ItemSearchCondition.none(), 1, 2);

		assertThat(page.getContent()).extracting(ItemSummaryResponse::name).containsExactly("물품2", "물품1");
		assertThat(page.getTotalElements()).isEqualTo(5);
		assertThat(page.getTotalPages()).isEqualTo(3);
		assertThat(page.isFirst()).isFalse();
		assertThat(page.isLast()).isFalse();
	}

	@Test
	void 물품_수와_상관없이_쿼리는_최대_4번만_나간다() {
		for (int i = 0; i < 10; i++) {
			Item item = persist(item("물품" + i, ItemStatus.OPEN));
			persist(image(item, "photo" + i + ".jpg", 0));
			persist(tradeMethod(item, TradeMethod.DIRECT));
		}
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		// 물품(10개)이 페이지 크기(5)보다 많아야 전체 개수 쿼리도 나간다
		// (한 페이지에 다 들어오면 Spring Data가 개수 쿼리를 생략해서 3번)
		itemService.getItems(ItemSearchCondition.none(), 0, 5);

		// 목록 + 전체 개수 + 사진 + 거래 방식 — 물품마다 사진·거래 방식을 따로 조회하지 않음
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
	}

	@Test
	void 물품명_검색은_부분_일치이고_퍼센트_기호는_글자_그대로_찾는다() {
		Item microwave = persist(item("전자레인지 20L", ItemStatus.OPEN));
		persist(item("책상", ItemStatus.OPEN));
		Item percent = persist(item("100% 새것 의자", ItemStatus.OPEN));
		flushAndClear();

		assertThat(names(search("레인지", null, null, null))).containsExactly(microwave.getName());
		assertThat(names(search("%", null, null, null))).containsExactly(percent.getName());
	}

	@Test
	void 카테고리와_거래_방식으로_거른다() {
		Item furnitureDirect = persist(item("가구-직거래", ItemStatus.OPEN, CategoryGroup.FURNITURE, "좋음", 30));
		persist(tradeMethod(furnitureDirect, TradeMethod.DIRECT));
		Item furnitureBoth = persist(item("가구-둘다", ItemStatus.OPEN, CategoryGroup.FURNITURE, "좋음", 30));
		persist(tradeMethod(furnitureBoth, TradeMethod.DIRECT));
		persist(tradeMethod(furnitureBoth, TradeMethod.CAMPAIGN));
		Item applianceCampaign = persist(item("가전-거점", ItemStatus.OPEN, CategoryGroup.APPLIANCE, "좋음", 24));
		persist(tradeMethod(applianceCampaign, TradeMethod.CAMPAIGN));
		flushAndClear();

		assertThat(names(search(null, CategoryGroup.FURNITURE, null, null)))
				.containsExactly("가구-둘다", "가구-직거래");
		assertThat(names(search(null, null, TradeMethod.CAMPAIGN, null)))
				.containsExactly("가전-거점", "가구-둘다");
		assertThat(names(search(null, CategoryGroup.FURNITURE, TradeMethod.CAMPAIGN, null)))
				.containsExactly("가구-둘다");
	}

	@Test
	void 탄소_절감량순은_많은_순이고_같으면_최신순() {
		persist(item("8kg", ItemStatus.OPEN, CategoryGroup.ETC, "좋음", 8));
		persist(item("30kg-먼저", ItemStatus.OPEN, CategoryGroup.FURNITURE, "좋음", 30));
		persist(item("24kg", ItemStatus.OPEN, CategoryGroup.APPLIANCE, "좋음", 24));
		persist(item("30kg-나중", ItemStatus.OPEN, CategoryGroup.FURNITURE, "좋음", 30));
		flushAndClear();

		assertThat(names(search(null, null, null, ItemSort.CARBON)))
				.containsExactly("30kg-나중", "30kg-먼저", "24kg", "8kg");
	}

	@Test
	void 상태순은_두_가지_표기를_모두_같은_순위로_본다() {
		persist(item("사용감", ItemStatus.OPEN, CategoryGroup.ETC, "사용감 있음", 8));
		persist(item("좋음", ItemStatus.OPEN, CategoryGroup.ETC, "좋음", 8));
		persist(item("거의 새것", ItemStatus.OPEN, CategoryGroup.ETC, "거의 새것", 8));
		persist(item("상태 좋음", ItemStatus.OPEN, CategoryGroup.ETC, "상태 좋음", 8));
		persist(item("매우 좋음", ItemStatus.OPEN, CategoryGroup.ETC, "매우 좋음", 8));
		flushAndClear();

		assertThat(names(search(null, null, null, ItemSort.CONDITION)))
				.containsExactly("매우 좋음", "거의 새것", "상태 좋음", "좋음", "사용감");
	}

	@Test
	void 관리자가_숨긴_물품은_어느_상태_범위에도_나오지_않는다() {
		Item shown = persist(item("보임", ItemStatus.OPEN));
		Item hidden = persist(item("숨김", ItemStatus.OPEN));
		hidden.hide();
		flushAndClear();

		for (ItemAvailability availability : ItemAvailability.values()) {
			Page<ItemSummaryResponse> page = itemService.getItems(availability(availability), 0, 20);

			assertThat(page.getContent()).extracting(ItemSummaryResponse::id).containsExactly(shown.getId());
			assertThat(page.getTotalElements()).isEqualTo(1);
		}
	}

	@Test
	void 필터를_걸어도_쿼리는_최대_4번만_나간다() {
		for (int i = 0; i < 10; i++) {
			Item item = persist(item("의자" + i, ItemStatus.OPEN, CategoryGroup.FURNITURE, "좋음", 30));
			persist(image(item, "photo" + i + ".jpg", 0));
			persist(tradeMethod(item, TradeMethod.DIRECT));
		}
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		Page<ItemSummaryResponse> page =
				itemService.getItems(search("의자", CategoryGroup.FURNITURE, TradeMethod.DIRECT, ItemSort.CARBON), 0, 5);

		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
	}

	private static ItemSearchCondition search(String keyword, CategoryGroup categoryGroup, TradeMethod tradeMethod,
			ItemSort sort) {
		return new ItemSearchCondition(keyword, categoryGroup, tradeMethod, sort);
	}

	private static ItemSearchCondition availability(ItemAvailability availability) {
		return new ItemSearchCondition(null, null, null, ItemSort.LATEST, availability);
	}

	private List<String> names(ItemSearchCondition condition) {
		return itemService.getItems(condition, 0, 20).getContent().stream().map(ItemSummaryResponse::name).toList();
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private Item item(String name, ItemStatus status) {
		return item(name, status, CategoryGroup.APPLIANCE, "좋음", 24);
	}

	private Item item(String name, ItemStatus status, CategoryGroup categoryGroup, String conditionGrade,
			int carbonReduction) {
		return Item.builder()
				.owner(owner)
				.name(name)
				.categoryGroup(categoryGroup)
				.category("세부")
				.conditionGrade(conditionGrade)
				.transportDifficulty("보통")
				.estimatedCarbonReduction(carbonReduction)
				.availableFrom(LocalDate.now())
				.availableUntil(LocalDate.now())
				.applicationDeadline(LocalDateTime.now().plusDays(3))
				.status(status)
				.build();
	}

	private static ItemImage image(Item item, String url, int displayOrder) {
		return ItemImage.builder().item(item).imageUrl(url).displayOrder(displayOrder).build();
	}

	private static ItemTradeMethod tradeMethod(Item item, TradeMethod tradeMethod) {
		return ItemTradeMethod.builder().item(item).tradeMethod(tradeMethod).build();
	}

}
