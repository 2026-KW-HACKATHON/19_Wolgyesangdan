package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
	void 취소와_등록대기_물품은_빼고_최신순으로_내려준다() {
		Item open = persist(item("열림", ItemStatus.OPEN));
		persist(item("등록대기", ItemStatus.REGISTERED));
		Item closed = persist(item("마감", ItemStatus.CLOSED));
		persist(item("취소", ItemStatus.CANCELED));
		Item assigned = persist(item("예약중", ItemStatus.ASSIGNED));
		Item completed = persist(item("완료", ItemStatus.COMPLETED));
		flushAndClear();

		Page<ItemSummaryResponse> page = itemService.getItems(0, 20);

		assertThat(page.getContent()).extracting(ItemSummaryResponse::id)
				.containsExactly(completed.getId(), assigned.getId(), closed.getId(), open.getId());
		assertThat(page.getTotalElements()).isEqualTo(4);
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

		List<ItemSummaryResponse> content = itemService.getItems(0, 20).getContent();

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

		Page<ItemSummaryResponse> page = itemService.getItems(1, 2);

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
		itemService.getItems(0, 5);

		// 목록 + 전체 개수 + 사진 + 거래 방식 — 물품마다 사진·거래 방식을 따로 조회하지 않음
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
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
		return Item.builder()
				.owner(owner)
				.name(name)
				.categoryGroup(CategoryGroup.APPLIANCE)
				.category("생활가전")
				.conditionGrade("좋음")
				.transportDifficulty("보통")
				.estimatedCarbonReduction(24)
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
