package com.mahendra.bizcart_backend.authentication.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private static final String ERROR_CODE = "AUTH_ACCESS_DENIED";
	private static final String ERROR_MESSAGE = "Access is denied";

	private final ObjectMapper objectMapper;

	public RestAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), Map.of(
				"timestamp", Instant.now().toString(),
				"status", HttpStatus.FORBIDDEN.value(),
				"error", HttpStatus.FORBIDDEN.getReasonPhrase(),
				"code", ERROR_CODE,
				"message", ERROR_MESSAGE,
				"path", request.getRequestURI()));
	}
}
