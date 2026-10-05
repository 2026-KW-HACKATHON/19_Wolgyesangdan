package com.Wolgyesangdan.backend.domain.carbonreport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.application.entity.Application;
import com.Wolgyesangdan.backend.domain.application.entity.ApplicationStatus;
import com.Wolgyesangdan.backend.domain.carbonreport.dto.MyImpactResponse;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.item.entity.TradeMethod;
import com.Wolgyesangdan.backend.domain.reservation.entity.Reservation;
import com.Wolgyesangdan.backend.domain.reservation.entity.ReservationStatus;
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

/**
 * 실제 MySQL에서 나의 자원순환 기록 집계 확인. 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, CarbonReportService.class})
class MyImpactQueryTest {

	@Autowired
	private CarbonReportService carbonReportService;

	@Autowired
	private EntityManager entityManager;

	private User me;

	@BeforeEach
	void setUp() {
		me = persist(user("나"));
	}

	@Test
	void 전달_완료와_수령_완료를_따로_세고_탄소는_둘을_합친다() {
		User other = persist(user("이웃"));
		// 내가 등록 → 이웃이 받음 (전달 2건)
		reservation(item(me, 30), other, ReservationStatus.COMPLETED);
		reservation(item(me, 24), other, ReservationStatus.COMPLETED);
		// 이웃이 등록 → 내가 받음 (수령 1건)
		reservation(item(other, 10), me, ReservationStatus.COMPLETED);
		flushAndClear();

		MyImpactResponse result = carbonReportService.getMyImpact(me.getId());

		assertThat(result).isEqualTo(new MyImpactResponse(2, 1, 64));
	}

	@Test
	void 거래가_완료되지_않은_예약은_세지_않는다() {
		User other = persist(user("이웃"));
		reservation(item(me, 30), other, ReservationStatus.SCHEDULED);
		reservation(item(me, 30), other, ReservationStatus.RECONFIRMED);
		reservation(item(other, 10), me, ReservationStatus.NO_SHOW);
		reservation(item(other, 10), me, ReservationStatus.CANCELED);
		flushAndClear();

		assertThat(carbonReportService.getMyImpact(me.getId())).isEqualTo(new MyImpactResponse(0, 0, 0));
	}

	@Test
	void 다른_사람끼리의_거래는_세지_않는다() {
		User a = persist(user("이웃A"));
		User b = persist(user("이웃B"));
		reservation(item(a, 30), b, ReservationStatus.COMPLETED);
		reservation(item(me, 8), a, ReservationStatus.COMPLETED);
		flushAndClear();

		assertThat(carbonReportService.getMyImpact(me.getId())).isEqualTo(new MyImpactResponse(1, 0, 8));
	}

	@Test
	void 본인_물품에_본인이_신청한_거래는_전달_수령_어느_쪽에도_세지_않는다() {
		User other = persist(user("이웃"));
		reservation(item(me, 30), me, ReservationStatus.COMPLETED);
		reservation(item(me, 24), other, ReservationStatus.COMPLETED);
		flushAndClear();

		assertThat(carbonReportService.getMyImpact(me.getId())).isEqualTo(new MyImpactResponse(1, 0, 24));
	}

	@Test
	void 거래_내역이_없으면_전부_0() {
		assertThat(carbonReportService.getMyImpact(me.getId())).isEqualTo(new MyImpactResponse(0, 0, 0));
	}

	@Test
	void 거래_건수와_관계없이_쿼리_한_번으로_계산한다() {
		User other = persist(user("이웃"));
		for (int i = 0; i < 5; i++) {
			reservation(item(me, 30), other, ReservationStatus.COMPLETED);
			reservation(item(other, 10), me, ReservationStatus.COMPLETED);
		}
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		MyImpactResponse result = carbonReportService.getMyImpact(me.getId());

		assertThat(result).isEqualTo(new MyImpactResponse(5, 5, 200));
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	private Item item(User owner, int carbonKg) {
		return persist(Item.builder()
				.owner(owner)
				.name("물품")
				.categoryGroup(CategoryGroup.FURNITURE)
				.conditionGrade("상태 좋음")
				.estimatedCarbonReduction(carbonKg)
				.availableFrom(LocalDate.now())
				.applicationDeadline(LocalDateTime.now().plusDays(3))
				.status(ItemStatus.COMPLETED)
				.build());
	}

	private void reservation(Item item, User applicant, ReservationStatus status) {
		Application application = persist(Application.builder()
				.item(item).applicant(applicant).priorityScore(0).status(ApplicationStatus.SELECTED).build());
		persist(Reservation.builder()
				.application(application).tradeMethod(TradeMethod.DIRECT).status(status)
				.completedAt(status == ReservationStatus.COMPLETED ? LocalDateTime.now() : null)
				.build());
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private static User user(String nickname) {
		return User.builder().kakaoId("test-" + UUID.randomUUID()).nickname(nickname).build();
	}

}
