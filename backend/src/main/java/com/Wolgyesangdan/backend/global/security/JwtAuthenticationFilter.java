package com.Wolgyesangdan.backend.global.security;

import java.io.IOException;
import java.util.List;

import com.Wolgyesangdan.backend.global.exception.BusinessException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 헤더의 access 토큰을 검증해서 SecurityContext에 userId를 principal로 넣는다.
 *
 * 토큰이 잘못돼도 여기서 바로 401을 내지 않고 에러 코드만 request에 남겨둔다 — 비회원 허용 API는
 * 토큰이 이상해도 비회원으로 그냥 통과시키고, 인증 필요 API에서만 JwtAuthenticationEntryPoint가
 * 이 에러 코드로 401을 응답한다.
 *
 * SecurityConfig에서 직접 생성해 등록하므로 @Component를 붙이지 않는다 (붙이면 서블릿 필터로 한 번 더 등록됨).
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTH_ERROR_CODE_ATTRIBUTE = "authErrorCode";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = resolveToken(request);
		if (token != null) {
			try {
				Long userId = jwtProvider.getUserIdFromAccessToken(token);
				SecurityContextHolder.getContext()
						.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of()));
			} catch (BusinessException e) {
				request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, e.getErrorCode());
			}
		}
		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return header.substring(BEARER_PREFIX.length());
	}

}
