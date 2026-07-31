package com.mahendra.bizcart_backend.user.dto.response;

public record UserResponseDto<T>(
		String message,
		T data
) {
}
