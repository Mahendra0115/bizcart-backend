package com.mahendra.bizcart_backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
		String timestamp,
		int status,
		String error,
		String code,
		String message,
		String path,
		List<FieldErrorResponse> fieldErrors) {
}
