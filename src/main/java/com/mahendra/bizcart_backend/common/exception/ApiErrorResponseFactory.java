package com.mahendra.bizcart_backend.common.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.slf4j.MDC;

public final class ApiErrorResponseFactory {

	private ApiErrorResponseFactory() {
	}

	public static ApiErrorResponse of(HttpStatus status, String code, String message, String path) {
		return of(status, code, message, path, List.of());
	}

	public static ApiErrorResponse of(HttpStatus status, String code, String message, String path,
			List<FieldErrorResponse> fieldErrors) {
		return new ApiErrorResponse(Instant.now().toString(), status.value(), status.getReasonPhrase(), code, message,
				path, MDC.get("correlationId"), fieldErrors);
	}
}
