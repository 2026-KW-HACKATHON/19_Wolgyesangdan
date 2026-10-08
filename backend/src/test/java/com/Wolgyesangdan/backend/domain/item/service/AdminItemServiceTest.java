package com.Wolgyesangdan.backend.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.application.service.AssignmentService;
import com.Wolgyesangdan.backend.domain.item.dto.AdminItemResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.exception.ItemErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 실제 MySQL에서 관리자 물품 관리 확인. 각 테스트는 물품 관련 테이블을 비운 상태에서 시작하고, 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, AdminItemService.class, AssignmentService.class})
class AdminItemServiceTest {

	@Autowired
	private AdminItemService adminItemService;

	@Autowired
	private EntityManager entityManager;

	private User owner;

	@BeforeEach
	void setUp() {
		for (String entity : List.of("Reservation", "Application", "ItemImage", "ItemTradeMethod", "Item")) {
			entityManager.createQuery("delete from " + entity).executeUpdate();
		}
		owner = persist(User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("등록자").build());
	}

	@Test
	void 새로_등록한_물품은_숨김이_아니다() {
		Item item = persist(item("전자레인지", ItemStatus.OPEN));
		flushAndClear();

		assertThat(entityManager.find(Item.class, item.getId()).isHidden()).isFalse();
	}

	@Test
	void 상태와_숨김_여부에_관계없이_전부_최근_등록순으로_내려준다() {
		Item open = persist(item("열림", ItemStatus.OPEN));
		Item canceled = persist(item("취소", ItemStatus.CANCELED));
		Item hidden = persist(item("숨김", ItemStatus.COMPLETED));
		hidden.hide();
		flushAndClear();

		Page<AdminItemResponse> page = adminItemService.getItems(null, PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(AdminItemResponse::id)
				.containsExactly(hidden.getId(), canceled.getId(), open.getId());
		AdminItemResponse first = page.getContent().getFirst();
		assertThat(first.name()).isEqualTo("숨김");
		assertThat(first.ownerNickname()).isEqualTo("등록자");
		assertThat(first.categoryGroup()).isEqualTo(CategoryGroup.APPLIANCE);
		assertThat(first.status()).isEqualTo(ItemStatus.COMPLETED);
		assertThat(first.createdAt()).isNotNull();
		assertThat(first.hidden()).isTrue();
	}

	@Test
	void hidden으로_숨긴_물품만_또는_보이는_물품만_거른다() {
		Item shown = persist(item("보임", ItemStatus.OPEN));
		Item hidden = persist(item("숨김", ItemStatus.OPEN));
		hidden.hide();
		flushAndClear();

		assertThat(adminItemService.getItems(true, PageRequest.of(0, 20)).getContent()).extracting(AdminItemResponse::id)
				.containsExactly(hidden.getId());
		assertThat(adminItemService.getItems(false, PageRequest.of(0, 20)).getContent()).extracting(AdminItemResponse::id)
				.containsExactly(shown.getId());
	}

	@Test
	void 페이지를_나눠서_내려준다() {
		for (int i = 0; i < 5; i++) {
			persist(item("물품" + i, ItemStatus.OPEN));
		}
		flushAndClear();

		Page<AdminItemResponse> page = adminItemService.getItems(null, PageRequest.of(1, 2));

		assertThat(page.getContent()).extracting(AdminItemResponse::name).containsExactly("물품2", "물품1");
		assertThat(page.getTotalElements()).isEqualTo(5);
		assertThat(page.getTotalPages()).isEqualTo(3);
	}

	@Test
	void 요청의_정렬은_무시하고_한_페이지는_100개까지만_내려준다() {
		Item first = persist(item("가", ItemStatus.OPEN));
		Item second = persist(item("나", ItemStatus.OPEN));
		flushAndClear();

		Page<AdminItemResponse> page = adminItemService.getItems(null, PageRequest.of(0, 1000, Sort.by("name")));

		assertThat(page.getContent()).extracting(AdminItemResponse::id).containsExactly(second.getId(), first.getId());
		assertThat(page.getSize()).isEqualTo(100);
	}

	@Test
	void 등록자를_물품마다_따로_조회하지_않는다() {
		for (int i = 0; i < 5; i++) {
			User other = persist(User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("등록자" + i).build());
			persist(Item.builder().owner(other).name("물품" + i).categoryGroup(CategoryGroup.APPLIANCE)
					.conditionGrade("좋음").estimatedCarbonReduction(24)
					.applicationDeadline(LocalDateTime.now().plusDays(3)).status(ItemStatus.OPEN).build());
		}
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		adminItemService.getItems(null, PageRequest.of(0, 2));

		// 물품+등록자, 전체 개수
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void 숨겼다가_다시_보이게_해도_물품_상태는_그대로다() {
		Item item = persist(item("전자레인지", ItemStatus.ASSIGNED));
		flushAndClear();

		AdminItemResponse hiddenResponse = adminItemService.changeHidden(item.getId(), true);
		flushAndClear();

		assertThat(hiddenResponse.hidden()).isTrue();
		Item hidden = entityManager.find(Item.class, item.getId());
		assertThat(hidden.isHidden()).isTrue();
		assertThat(hidden.getStatus()).isEqualTo(ItemStatus.ASSIGNED);

		AdminItemResponse shownResponse = adminItemService.changeHidden(item.getId(), false);
		flushAndClear();

		assertThat(shownResponse.hidden()).isFalse();
		assertThat(entityManager.find(Item.class, item.getId()).isHidden()).isFalse();
	}

	@Test
	void 없는_물품이면_ITEM_NOT_FOUND() {
		assertThatThrownBy(() -> adminItemService.changeHidden(Long.MAX_VALUE, true))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ItemErrorCode.ITEM_NOT_FOUND);
	}

	@Test
	void 신청을_조기_마감하면_1순위에게_배정되고_바뀐_상태를_내려준다() {
		Item item = persist(item("전자레인지", ItemStatus.OPEN));
		User applicant = persist(User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("신청자").build());
		Application application = persist(Application.builder()
				.item(item).applicant(applicant).priorityScore(0).status(ApplicationStatus.WAITING).build());
		flushAndClear();

		AdminItemResponse response = adminItemService.closeApplications(item.getId());
		flushAndClear();

		assertThat(response.status()).isEqualTo(ItemStatus.ASSIGNED);
		assertThat(entityManager.find(Item.class, item.getId()).getStatus()).isEqualTo(ItemStatus.ASSIGNED);
		assertThat(entityManager.find(Application.class, application.getId()).getStatus())
				.isEqualTo(ApplicationStatus.SELECTED);
	}

	@Test
	void 신청자가_없는_물품을_조기_마감하면_종료된_상태를_내려준다() {
		Item item = persist(item("전자레인지", ItemStatus.OPEN));
		flushAndClear();

		assertThat(adminItemService.closeApplications(item.getId()).status()).isEqualTo(ItemStatus.CANCELED);
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
				.conditionGrade("좋음")
				.estimatedCarbonReduction(24)
				.applicationDeadline(LocalDateTime.now().plusDays(3))
				.status(status)
				.build();
	}

}
