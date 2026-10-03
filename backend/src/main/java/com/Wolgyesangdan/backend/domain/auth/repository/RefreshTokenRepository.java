package com.Wolgyesangdan.backend.domain.auth.repository;

import java.time.LocalDateTime;

import com.Wolgyesangdan.backend.domain.auth.entity.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	/**
	 * 삭제된 행 수를 반환한다. 0이면 이미 폐기됐거나 없는 토큰 —
	 * 같은 토큰으로 재발급 요청이 동시에 두 번 와도 한 쪽만 성공한다.
	 */
	@Modifying
	@Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
	int deleteByTokenHash(@Param("tokenHash") String tokenHash);

	@Modifying
	@Query("delete from RefreshToken r where r.user.id = :userId and r.expiresAt < :now")
	void deleteExpiredByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

}
