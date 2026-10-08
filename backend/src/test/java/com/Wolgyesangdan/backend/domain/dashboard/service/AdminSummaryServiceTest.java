package com.Wolgyesangdan.backend.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.service.CampaignService;
import com.Wolgyesangdan.backend.domain.dashboard.dto.AdminSummaryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;
import com.Wolgyesangdan.backend.domain.item.entity.Item;
import com.Wolgyesangdan.backend.domain.item.entity.ItemStatus;
import com.Wolgyesangdan.backend.domain.user.entity.User;
import com.Wolgyesangdan.backend.domain.verification.entity.PriorityVerification;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationStatus;
import com.Wolgyesangdan.backend.domain.verification.entity.VerificationType;
import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * 실제 MySQL에서 대시보드 요약 확인. 끝나면 롤백된다.
 * DB에 이미 있는 데이터와 섞이지 않도록 건수는 "넣기 전과의 차이"로 확인하고,
 * 캠페인은 기존 캠페인이 모두 끝났을 먼 미래 날짜를 오늘로 준다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, AdminSummaryService.class, CampaignService.class})
class AdminSummaryServiceTest {

	private static final LocalDate FAR_FUTURE = LocalDate.of(2099, 6, 15);

	@Autowired
	private AdminSummaryService adminSummaryService;

	@Autowired
	private EntityManager entityManager;

	private User member;
	private User admin;

	@BeforeEach
	void setUp() {
		member = persist(user("회원"));
		admin = persist(user("운영자"));
	}

	@Test
	void 검토_대기_서류는_우선배정_유형의_PENDING만_센다() {
		long before = adminSummaryService.getSummary().pendingVerifications();
		persist(verification(VerificationType.FRESHMAN, VerificationStatus.PENDING));
		persist(verification(VerificationType.LOW_INCOME, VerificationStatus.PENDING));
		persist(verification(VerificationType.FRESHMAN, VerificationStatus.APPROVED));
		persist(verification(VerificationType.LOW_INCOME, VerificationStatus.REJECTED));
		// 동네 인증은 심사 대상이 아니라 관리자 서류 화면에 나오지 않는다
		persist(verification(VerificationType.NEIGHBORHOOD, VerificationStatus.PENDING));
		flushAndClear();

		assertThat(adminSummaryService.getSummary().pendingVerifications()).isEqualTo(before + 2);
	}

	@Test
	void 답변_대기_문의는_답변하지_않은_문의만_센다() {
		long before = adminSummaryService.getSummary().openInquiries();
		persist(inquiry());
		persist(inquiry());
		Inquiry answered = persist(inquiry());
		answered.answer("답변", admin, LocalDateTime.now());
		flushAndClear();

		assertThat(adminSummaryService.getSummary().openInquiries()).isEqualTo(before + 2);
	}

	@Test
	void 숨긴_물품만_센다() {
		long before = adminSummaryService.getSummary().hiddenItems();
		persist(item(null, ItemStatus.OPEN)).hide();
		persist(item(null, ItemStatus.COMPLETED)).hide();
		persist(item(null, ItemStatus.OPEN));
		flushAndClear();

		assertThat(adminSummaryService.getSummary().hiddenItems()).isEqualTo(before + 2);
	}

	@Test
	void 진행_중인_캠페인과_그_캠페인의_거래_완료_수를_내려준다() {
		Campaign campaign = persist(campaign(FAR_FUTURE.minusDays(5), FAR_FUTURE.plusDays(10)));
		persist(item(campaign, ItemStatus.COMPLETED));
		persist(item(campaign, ItemStatus.COMPLETED));
		persist(item(campaign, ItemStatus.OPEN));
		persist(item(null, ItemStatus.COMPLETED)); // 캠페인 물품이 아님
		flushAndClear();

		AdminSummaryResponse.CurrentCampaign current = adminSummaryService.getSummary(FAR_FUTURE).currentCampaign();

		assertThat(current.id()).isEqualTo(campaign.getId());
		assertThat(current.name()).isEqualTo("먼 미래 캠페인");
		assertThat(current.status()).isEqualTo(CampaignStatus.ACTIVE);
		assertThat(current.startDate()).isEqualTo(FAR_FUTURE.minusDays(5));
		assertThat(current.endDate()).isEqualTo(FAR_FUTURE.plusDays(12));
		assertThat(current.reusedCount()).isEqualTo(2);
	}

	@Test
	void 예정_캠페인도_내려준다() {
		Campaign campaign = persist(campaign(FAR_FUTURE.plusDays(3), FAR_FUTURE.plusDays(20)));
		flushAndClear();

		AdminSummaryResponse.CurrentCampaign current = adminSummaryService.getSummary(FAR_FUTURE).currentCampaign();

		assertThat(current.id()).isEqualTo(campaign.getId());
		assertThat(current.status()).isEqualTo(CampaignStatus.PLANNED);
		assertThat(current.reusedCount()).isZero();
	}

	@Test
	void 예정이거나_진행_중인_캠페인이_없으면_currentCampaign은_null() {
		Campaign closed = persist(campaign(FAR_FUTURE.minusDays(5), FAR_FUTURE.plusDays(10)));
		closed.end(); // 운영 중을 끈 캠페인
		flushAndClear();

		assertThat(adminSummaryService.getSummary(FAR_FUTURE).currentCampaign()).isNull();
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

	private PriorityVerification verification(VerificationType type, VerificationStatus status) {
		// 유형별로 회원을 따로 둬서 "같은 유형의 진행 중인 신청은 하나" 같은 제약과 부딪히지 않게 한다
		return PriorityVerification.builder()
				.user(persist(user("신청자")))
				.verificationType(type)
				.status(status)
				.submittedAt(LocalDateTime.now())
				.build();
	}

	private Inquiry inquiry() {
		return Inquiry.builder().author(member).category(InquiryCategory.ETC).title("제목").content("내용").build();
	}

	private Item item(Campaign campaign, ItemStatus status) {
		return Item.builder()
				.owner(member)
				.campaign(campaign)
				.name("전자레인지")
				.categoryGroup(CategoryGroup.APPLIANCE)
				.conditionGrade("좋음")
				.estimatedCarbonReduction(24)
				.applicationDeadline(LocalDateTime.now().plusDays(3))
				.status(status)
				.build();
	}

	private static Campaign campaign(LocalDate start, LocalDate end) {
		return Campaign.builder()
				.name("먼 미래 캠페인")
				.registrationStartDate(start).registrationEndDate(end)
				.applicationStartDate(start).applicationEndDate(end)
				.pickupStartDate(start).pickupEndDate(end.plusDays(2))
				.locationName("거점").locationAddress("주소")
				.status(CampaignStatus.ACTIVE)
				.build();
	}

}
