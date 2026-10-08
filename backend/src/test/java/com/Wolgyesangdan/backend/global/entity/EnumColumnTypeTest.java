package com.Wolgyesangdan.backend.global.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.Wolgyesangdan.backend.global.config.JpaAuditingConfig;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * enum 필드는 MySQL ENUM이 아니라 VARCHAR(30)으로 만들어져야 한다 (#36).
 * Hibernate 7은 @Enumerated(STRING)을 MySQL ENUM으로 만들기 때문에 필드마다 @JdbcTypeCode(SqlTypes.VARCHAR)를 붙인다.
 * 빠뜨리면 enum 값을 추가할 때마다 컬럼 정의를 바꿔야 하고, 기동할 때마다 ALTER TABLE이 돈다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class EnumColumnTypeTest {

	@Autowired
	private EntityManager entityManager;

	@Test
	void MySQL_ENUM_타입_컬럼이_없다() {
		List<?> enumColumns = entityManager.createNativeQuery("""
						select concat(table_name, '.', column_name) from information_schema.columns
						where table_schema = database() and data_type = 'enum'
						""")
				.getResultList();

		assertThat(enumColumns).isEmpty();
	}

	@Test
	void enum_필드_컬럼은_VARCHAR_30이다() {
		List<?> columnTypes = entityManager.createNativeQuery("""
						select column_type from information_schema.columns
						where table_schema = database()
							and (table_name, column_name) in (
								('applications', 'status'), ('campaigns', 'status'),
								('inquiries', 'category'), ('inquiries', 'status'),
								('item_trade_methods', 'trade_method'), ('items', 'status'),
								('priority_verifications', 'status'), ('priority_verifications', 'verification_type'),
								('priority_verifications', 'document_type'),
								('reservations', 'status'), ('reservations', 'trade_method'),
								('users', 'contact_type'), ('users', 'role'),
								('verification_audit_logs', 'action'))
						""")
				.getResultList();

		assertThat(columnTypes).hasSize(14).allMatch("varchar(30)"::equals);
	}

}
