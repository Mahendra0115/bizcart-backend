package com.mahendra.bizcart_backend.authentication.dto.response;

public record AuthResponseDto<T>(
		String message,
		T data
) {
}
