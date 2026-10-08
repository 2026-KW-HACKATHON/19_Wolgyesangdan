package com.Wolgyesangdan.backend.global.security;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 로그인은 했지만 권한이 없는 API(관리자 API를 일반 회원이 호출 등)에 들어왔을 때 403 AUTH_FORBIDDEN.
 * JwtAuthenticationEntryPoint와 같은 이유로 HandlerExceptionResolver에 넘겨 응답 형식을 맞춘다.
 * SecurityConfig에서 직접 생성한다 (컨트롤러 테스트가 SecurityConfig만 가져와도 동작하도록).
 */
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final HandlerExceptionResolver handlerExceptionResolver;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) {
		handlerExceptionResolver.resolveException(request, response, null,
				new BusinessException(AuthErrorCode.AUTH_FORBIDDEN));
	}

}
