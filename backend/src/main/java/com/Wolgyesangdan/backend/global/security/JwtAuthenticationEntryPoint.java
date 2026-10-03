package com.Wolgyesangdan.backend.global.security;

import com.Wolgyesangdan.backend.domain.auth.exception.AuthErrorCode;
import com.Wolgyesangdan.backend.global.exception.BaseErrorCode;
import com.Wolgyesangdan.backend.global.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증 필요 API에 인증 없이 들어왔을 때 401 응답.
 * 응답 형식을 GlobalExceptionHandler와 똑같이 맞추려고, 직접 JSON을 쓰지 않고 HandlerExceptionResolver에 넘긴다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final HandlerExceptionResolver handlerExceptionResolver;

	public JwtAuthenticationEntryPoint(
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		BaseErrorCode errorCode =
				request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTRIBUTE) instanceof BaseErrorCode code
						? code
						: AuthErrorCode.AUTH_UNAUTHORIZED;
		handlerExceptionResolver.resolveException(request, response, null, new BusinessException(errorCode));
	}

}
