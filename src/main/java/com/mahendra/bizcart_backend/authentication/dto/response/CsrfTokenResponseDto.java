package com.mahendra.bizcart_backend.authentication.dto.response;

public record CsrfTokenResponseDto(
		String headerName,
		String parameterName,
		String token
) {
}
