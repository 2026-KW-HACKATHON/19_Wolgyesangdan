package com.Wolgyesangdan.backend.domain.user.repository;

import java.util.Optional;

import com.Wolgyesangdan.backend.domain.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKakaoId(String kakaoId);

}
