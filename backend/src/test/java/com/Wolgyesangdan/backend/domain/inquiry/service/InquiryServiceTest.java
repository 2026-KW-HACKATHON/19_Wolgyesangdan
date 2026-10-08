package com.Wolgyesangdan.backend.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquiryDetailResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.AdminInquirySummaryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryAnswerRequest;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryCreateRequest;
import com.Wolgyesangdan.backend.domain.inquiry.dto.InquiryResponse;
import com.Wolgyesangdan.backend.domain.inquiry.entity.Inquiry;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryCategory;
import com.Wolgyesangdan.backend.domain.inquiry.entity.InquiryStatus;
import com.Wolgyesangdan.backend.domain.inquiry.exception.InquiryErrorCode;
import com.Wolgyesangdan.backend.domain.user.entity.Role;
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
 * 실제 MySQL에서 문의 작성·조회·답변 확인. 각 테스트는 문의 테이블을 비운 상태에서 시작하고, 끝나면 롤백된다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, InquiryService.class, AdminInquiryService.class})
class InquiryServiceTest {

	@Autowired
	private InquiryService inquiryService;

	@Autowired
	private AdminInquiryService adminInquiryService;

	@Autowired
	private EntityManager entityManager;

	private User member;
	private User admin;

	@BeforeEach
	void setUp() {
		entityManager.createQuery("delete from Inquiry").executeUpdate();
		member = persist(user("문의자", Role.USER));
		admin = persist(user("운영자", Role.ADMIN));
	}

	// ── 회원 ──

	@Test
	void 문의를_작성하면_답변_대기_상태로_저장된다() {
		InquiryResponse response = inquiryService.createInquiry(member.getId(),
				new InquiryCreateRequest(InquiryCategory.HUB, " 거점 운영 시간 ", " 주말에도 여나요? "));
		flushAndClear();

		Inquiry saved = entityManager.find(Inquiry.class, response.id());
		assertThat(saved.getAuthor().getId()).isEqualTo(member.getId());
		assertThat(saved.getCategory()).isEqualTo(InquiryCategory.HUB);
		assertThat(saved.getTitle()).isEqualTo("거점 운영 시간");
		assertThat(saved.getContent()).isEqualTo("주말에도 여나요?");
		assertThat(saved.getStatus()).isEqualTo(InquiryStatus.OPEN);
		assertThat(saved.getAnswer()).isNull();
		assertThat(response.status()).isEqualTo(InquiryStatus.OPEN);
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	void 없는_회원이_문의를_작성하면_AUTH_USER_NOT_FOUND() {
		assertThatThrownBy(() -> inquiryService.createInquiry(Long.MAX_VALUE,
				new InquiryCreateRequest(InquiryCategory.ETC, "제목", "내용")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(AuthErrorCode.AUTH_USER_NOT_FOUND);
	}

	@Test
	void 내_문의만_최근에_쓴_순으로_답변과_함께_내려준다() {
		Inquiry first = persist(inquiry(member, "첫 문의"));
		Inquiry second = persist(inquiry(member, "둘째 문의"));
		persist(inquiry(persist(user("다른 회원", Role.USER)), "남의 문의"));
		first.answer("답변입니다", admin, java.time.LocalDateTime.now());
		flushAndClear();

		Page<InquiryResponse> page = inquiryService.getMyInquiries(member.getId(), PageRequest.of(0, 20, Sort.by("title")));

		assertThat(page.getContent()).extracting(InquiryResponse::id).containsExactly(second.getId(), first.getId());
		assertThat(page.getContent().get(0).answer()).isNull();
		InquiryResponse answered = page.getContent().get(1);
		assertThat(answered.status()).isEqualTo(InquiryStatus.ANSWERED);
		assertThat(answered.answer()).isEqualTo("답변입니다");
		assertThat(answered.answeredAt()).isNotNull();
	}

	@Test
	void 내_문의는_한_페이지에_100개까지만_내려준다() {
		persist(inquiry(member, "문의"));
		flushAndClear();

		assertThat(inquiryService.getMyInquiries(member.getId(), PageRequest.of(0, 1000)).getSize()).isEqualTo(100);
	}

	// ── 관리자 ──

	@Test
	void 관리자_목록은_답변_대기가_먼저이고_그_안에서는_최근_순이다() {
		Inquiry answeredOld = persist(inquiry(member, "답변한 옛 문의"));
		Inquiry openOld = persist(inquiry(member, "대기 옛 문의"));
		Inquiry answeredNew = persist(inquiry(member, "답변한 새 문의"));
		Inquiry openNew = persist(inquiry(member, "대기 새 문의"));
		answeredOld.answer("답변", admin, java.time.LocalDateTime.now());
		answeredNew.answer("답변", admin, java.time.LocalDateTime.now());
		flushAndClear();

		Page<AdminInquirySummaryResponse> page = adminInquiryService.getInquiries(null, PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(AdminInquirySummaryResponse::id)
				.containsExactly(openNew.getId(), openOld.getId(), answeredNew.getId(), answeredOld.getId());
		AdminInquirySummaryResponse first = page.getContent().getFirst();
		assertThat(first.category()).isEqualTo(InquiryCategory.TRADE);
		assertThat(first.title()).isEqualTo("대기 새 문의");
		assertThat(first.nickname()).isEqualTo("문의자");
		assertThat(first.createdAt()).isNotNull();
		assertThat(first.status()).isEqualTo(InquiryStatus.OPEN);
	}

	@Test
	void 관리자_목록을_상태로_거르고_페이지를_나눈다() {
		Inquiry open1 = persist(inquiry(member, "대기1"));
		Inquiry open2 = persist(inquiry(member, "대기2"));
		Inquiry open3 = persist(inquiry(member, "대기3"));
		Inquiry answered = persist(inquiry(member, "답변"));
		answered.answer("답변", admin, java.time.LocalDateTime.now());
		flushAndClear();

		Page<AdminInquirySummaryResponse> open = adminInquiryService.getInquiries(InquiryStatus.OPEN, PageRequest.of(1, 2));

		assertThat(open.getContent()).extracting(AdminInquirySummaryResponse::id).containsExactly(open1.getId());
		assertThat(open.getTotalElements()).isEqualTo(3);
		assertThat(open.getTotalPages()).isEqualTo(2);
		assertThat(adminInquiryService.getInquiries(InquiryStatus.ANSWERED, PageRequest.of(0, 20)).getContent())
				.extracting(AdminInquirySummaryResponse::id).containsExactly(answered.getId());
		assertThat(adminInquiryService.getInquiries(InquiryStatus.OPEN, PageRequest.of(0, 2)).getContent())
				.extracting(AdminInquirySummaryResponse::id).containsExactly(open3.getId(), open2.getId());
	}

	@Test
	void 관리자_목록은_작성자를_문의마다_따로_조회하지_않는다() {
		for (int i = 0; i < 5; i++) {
			persist(inquiry(persist(user("회원" + i, Role.USER)), "문의" + i));
		}
		flushAndClear();
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		adminInquiryService.getInquiries(null, PageRequest.of(0, 2));

		// 문의+작성자, 전체 개수
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void 관리자_상세는_본문과_답변을_함께_내려준다() {
		Inquiry inquiry = persist(inquiry(member, "제목"));
		flushAndClear();

		AdminInquiryDetailResponse response = adminInquiryService.getInquiry(inquiry.getId());

		assertThat(response.title()).isEqualTo("제목");
		assertThat(response.nickname()).isEqualTo("문의자");
		assertThat(response.content()).isEqualTo("문의 내용");
		assertThat(response.status()).isEqualTo(InquiryStatus.OPEN);
		assertThat(response.answer()).isNull();
		assertThat(response.answeredAt()).isNull();
	}

	@Test
	void 답변하면_답변_완료가_되고_누가_언제_답했는지_기록된다() {
		Inquiry inquiry = persist(inquiry(member, "제목"));
		flushAndClear();

		AdminInquiryDetailResponse response = adminInquiryService.answer(admin.getId(), inquiry.getId(),
				new InquiryAnswerRequest(" 주말에는 쉽니다. "));
		flushAndClear();

		assertThat(response.status()).isEqualTo(InquiryStatus.ANSWERED);
		assertThat(response.answer()).isEqualTo("주말에는 쉽니다.");
		assertThat(response.answeredAt()).isNotNull();
		Inquiry saved = entityManager.find(Inquiry.class, inquiry.getId());
		assertThat(saved.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
		assertThat(saved.getAnswer()).isEqualTo("주말에는 쉽니다.");
		assertThat(saved.getAnsweredBy().getId()).isEqualTo(admin.getId());
		assertThat(saved.getAnsweredAt()).isNotNull();
	}

	@Test
	void 이미_답변한_문의에_다시_답변하면_409_INQUIRY_ALREADY_ANSWERED이고_답변은_그대로다() {
		Inquiry inquiry = persist(inquiry(member, "제목"));
		adminInquiryService.answer(admin.getId(), inquiry.getId(), new InquiryAnswerRequest("첫 답변"));
		flushAndClear();

		assertThatThrownBy(() -> adminInquiryService.answer(admin.getId(), inquiry.getId(), new InquiryAnswerRequest("고친 답변")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(InquiryErrorCode.INQUIRY_ALREADY_ANSWERED);
		assertThat(entityManager.find(Inquiry.class, inquiry.getId()).getAnswer()).isEqualTo("첫 답변");
	}

	@Test
	void 없는_문의면_404_INQUIRY_NOT_FOUND() {
		assertThatThrownBy(() -> adminInquiryService.getInquiry(Long.MAX_VALUE))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(InquiryErrorCode.INQUIRY_NOT_FOUND);
		assertThatThrownBy(() -> adminInquiryService.answer(admin.getId(), Long.MAX_VALUE, new InquiryAnswerRequest("답변")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(InquiryErrorCode.INQUIRY_NOT_FOUND);
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private static User user(String nickname, Role role) {
		return User.builder().kakaoId("test-" + UUID.randomUUID()).nickname(nickname).role(role).build();
	}

	private static Inquiry inquiry(User author, String title) {
		return Inquiry.builder().author(author).category(InquiryCategory.TRADE).title(title).content("문의 내용").build();
	}

}
