package com.mahendra.bizcart_backend.authentication.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final String ERROR_CODE = "AUTH_INVALID_TOKEN";
	private static final String ERROR_MESSAGE = "Authentication is required";

	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), Map.of(
				"timestamp", Instant.now().toString(),
				"status", HttpStatus.UNAUTHORIZED.value(),
				"error", HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				"code", ERROR_CODE,
				"message", ERROR_MESSAGE,
				"path", request.getRequestURI()));
	}
}
