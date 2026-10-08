package com.Wolgyesangdan.backend.global.config;

import java.util.List;

import com.Wolgyesangdan.backend.global.security.JwtAccessDeniedHandler;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationEntryPoint;
import com.Wolgyesangdan.backend.global.security.JwtAuthenticationFilter;
import com.Wolgyesangdan.backend.global.security.JwtProperties;
import com.Wolgyesangdan.backend.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * JWT 기반 stateless 인증 설정.
 *
 * 컨트롤러에서 로그인 사용자 id는 @AuthenticationPrincipal Long userId로 받는다.
 * (비회원 허용 API에서 비로그인 상태면 null)
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtProvider jwtProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// 비회원 허용 API — API 명세서에 "인증 불필요"로 적힌 것들
						.requestMatchers(HttpMethod.POST, "/auth/kakao", "/auth/token/refresh").permitAll()
						.requestMatchers(HttpMethod.GET, "/items", "/items/*", "/campaigns/active", "/carbon-report")
						.permitAll()
						// 로컬 개발용 임시 로그인 (DevAuthController, local 프로필에서만 존재)
						.requestMatchers("/dev/**").permitAll()
						// 헬스 체크 (로드밸런서·배포 확인용)
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.requestMatchers("/error").permitAll()
						// 관리자 API — 비로그인은 401, 일반 회원은 403
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(new JwtAccessDeniedHandler(handlerExceptionResolver)))
				.addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
