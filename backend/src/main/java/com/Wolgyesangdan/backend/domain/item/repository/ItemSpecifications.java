package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.Wolgyesangdan.backend.domain.item.dto.ItemSearchCondition;
import com.Wolgyesangdan.backend.domain.item.dto.ItemSort;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.ItemTradeMethod;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * 물품 목록 검색 조건을 JPA Criteria로 만든다.
 */
public final class ItemSpecifications {

	/**
	 * ⚠️ 상태 등급 값이 문서마다 다르다 (요구사항 명세서 미정 사항 8번).
	 * 값이 확정될 때까지 두 표기를 모두 같은 순위로 본다: 시안·프론트 / API·ERD
	 */
	private static final List<String> BEST_CONDITIONS = List.of("거의 새것", "매우 좋음");
	private static final List<String> GOOD_CONDITIONS = List.of("상태 좋음", "좋음");

	private ItemSpecifications() {
	}

	public static Specification<Item> search(Collection<ItemStatus> statuses, ItemSearchCondition condition) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(root.get("status").in(statuses));
			if (condition.keyword() != null) {
				predicates.add(cb.like(root.get("name"), "%" + escapeLike(condition.keyword()) + "%", '\\'));
			}
			if (condition.categoryGroup() != null) {
				predicates.add(cb.equal(root.get("categoryGroup"), condition.categoryGroup()));
			}
			if (condition.tradeMethod() != null) {
				Subquery<Long> supported = query.subquery(Long.class);
				Root<ItemTradeMethod> tradeMethod = supported.from(ItemTradeMethod.class);
				supported.select(tradeMethod.get("id")).where(
						cb.equal(tradeMethod.get("item"), root),
						cb.equal(tradeMethod.get("tradeMethod"), condition.tradeMethod()));
				predicates.add(cb.exists(supported));
			}
			// 개수 쿼리(Long)에는 정렬을 붙이지 않는다
			if (query.getResultType() != Long.class && query.getResultType() != long.class) {
				query.orderBy(orders(root, cb, condition.sort()));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static List<Order> orders(Root<Item> root, CriteriaBuilder cb, ItemSort sort) {
		List<Order> orders = new ArrayList<>();
		switch (sort) {
			case CARBON -> orders.add(cb.desc(root.get("estimatedCarbonReduction")));
			case CONDITION -> orders.add(cb.asc(conditionRank(root, cb)));
			case LATEST -> {
			}
		}
		orders.add(cb.desc(root.get("createdAt")));
		orders.add(cb.desc(root.get("id")));
		return orders;
	}

	private static Expression<Integer> conditionRank(Root<Item> root, CriteriaBuilder cb) {
		Expression<String> grade = root.get("conditionGrade");
		return cb.<Integer>selectCase()
				.when(grade.in(BEST_CONDITIONS), 0)
				.when(grade.in(GOOD_CONDITIONS), 1)
				.otherwise(2);
	}

	// 검색어에 섞인 %, _ 가 와일드카드로 동작하지 않게 한다
	private static String escapeLike(String keyword) {
		return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
