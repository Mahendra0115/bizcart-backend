package com.mahendra.bizcart_backend.authentication.dto.response;

public record LoginResponseDto(
		String accessToken,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn,
		CurrentUserResponseDto user
) {
}
