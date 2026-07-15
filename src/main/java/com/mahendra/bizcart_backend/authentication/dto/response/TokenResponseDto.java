package com.mahendra.bizcart_backend.authentication.dto.response;

public record TokenResponseDto(
		String accessToken,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {
}
