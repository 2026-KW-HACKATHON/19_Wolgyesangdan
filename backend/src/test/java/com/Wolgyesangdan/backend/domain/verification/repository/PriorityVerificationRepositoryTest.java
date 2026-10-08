package com.Wolgyesangdan.backend.domain.verification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

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
import org.springframework.data.domain.PageRequest;

// 로컬·CI 실제 DB를 쓰고 다른 테스트 데이터가 섞여 있을 수 있어서, 이 테스트가 만든 id가 포함되는지만 본다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class PriorityVerificationRepositoryTest {

	private static final EnumSet<VerificationType> PRIORITY = EnumSet.of(VerificationType.FRESHMAN,
			VerificationType.LOW_INCOME);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 3, 0);

	@Autowired
	private PriorityVerificationRepository priorityVerificationRepository;

	@Autowired
	private EntityManager entityManager;

	private User user;

	@BeforeEach
	void setUp() {
		user = User.builder().kakaoId("test-" + UUID.randomUUID()).nickname("테스터").build();
		entityManager.persist(user);
	}

	@Test
	void 관리자_목록은_우선배정_유형만_상태로_거른다() {
		Long pending = persist(VerificationType.FRESHMAN, VerificationStatus.PENDING, null, null);
		Long rejected = persist(VerificationType.LOW_INCOME, VerificationStatus.REJECTED, NOW, null);
		Long neighborhood = persist(VerificationType.NEIGHBORHOOD, VerificationStatus.APPROVED, null, null);

		List<Long> pendingIds = ids(VerificationStatus.PENDING);
		List<Long> allIds = ids(null);

		assertThat(pendingIds).contains(pending).doesNotContain(rejected, neighborhood);
		assertThat(allIds).contains(pending, rejected).doesNotContain(neighborhood);
	}

	@Test
	void 검토_후_기준_시각이_지나고_서류가_남은_신청만_찾는다() {
		String fileKey = "verifications/2026/10/01/0b6f3c2e-5d1a-4c8e-9f7a-1234567890ab.jpg";
		Long old = persist(VerificationType.FRESHMAN, VerificationStatus.APPROVED, NOW.minusDays(31), fileKey);
		Long recent = persist(VerificationType.FRESHMAN, VerificationStatus.APPROVED, NOW.minusDays(29), fileKey);
		Long pending = persist(VerificationType.FRESHMAN, VerificationStatus.PENDING, null, fileKey);
		Long purged = persist(VerificationType.LOW_INCOME, VerificationStatus.REJECTED, NOW.minusDays(40), null);

		assertThat(priorityVerificationRepository.findIdsWithDocumentReviewedBefore(NOW.minusDays(30)))
				.contains(old)
				.doesNotContain(recent, pending, purged);
	}

	private List<Long> ids(VerificationStatus status) {
		return priorityVerificationRepository.findForAdmin(PRIORITY, status, PageRequest.of(0, 1000))
				.map(PriorityVerification::getId).getContent();
	}

	private Long persist(VerificationType type, VerificationStatus status, LocalDateTime reviewedAt, String fileKey) {
		PriorityVerification verification = PriorityVerification.builder()
				.user(user)
				.verificationType(type)
				.status(status)
				.reviewedAt(reviewedAt)
				.fileKey(fileKey)
				.build();
		entityManager.persist(verification);
		return verification.getId();
	}

}
